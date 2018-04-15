package ru.spbstu.videomood;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;

import ru.spbstu.videomood.activities.VideoActivity;
import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Command;
import ru.spbstu.videomood.btservice.Constants;
import ru.spbstu.videomood.btservice.ControlPacket;
import ru.spbstu.videomood.btservice.DataPacket;
import ru.spbstu.videomood.btservice.Packet;
import ru.spbstu.videomood.btservice.VideosPacket;

public class AdminDeviceManager implements LifecycleObserver  {
    private static final String TAG = "VideoMood:AdminManager";

    private WeakReference<VideoActivity> activityRef;

    private DataPacket dataPacket;

    private MuseDataRepository repository;

    public AdminDeviceManager(VideoActivity videoActivity, MuseDataRepository repository) {
        activityRef = new WeakReference<>(videoActivity);
        this.repository = repository;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onStart() {
        Log.d(TAG, "start event occurred");
        if (mBtService == null) {
            setupBtService();
        }

        VideoActivity videoActivity = activityRef.get();
        videoActivity.getDataPacket().observe(videoActivity, new Observer<DataPacket>() {
            @Override
            public void onChanged(@Nullable DataPacket dataPacket) {
                AdminDeviceManager.this.dataPacket = dataPacket;
            }
        });

        if (mBtService != null) {
            // Only if the connectionState is STATE_NONE, do we know that we haven't started already
            if (mBtService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth service
                mBtService.startServer();
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onStop() {
        Log.d(TAG, "stop event occurred");
        if (mBtService != null) {
            mBtService.stop();
        }
    }

    private BluetoothService mBtService = null;

    static class MyHandler extends Handler {
        private WeakReference<AdminDeviceManager> weakManager;

        MyHandler(AdminDeviceManager manager) {
            weakManager = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            AdminDeviceManager manager = weakManager.get();
            if (manager != null)
                switch (msg.what) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        manager.processAdminDeviceState(msg.arg1);
                        break;
                    case Constants.MESSAGE_PACKET:
                        try {
                            if (msg.obj instanceof ControlPacket)
                                manager.processPacket((ControlPacket) msg.obj);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
        }
    }

    private void setupBtService() {
        MyHandler mAdminDeviceMessageHandler = new MyHandler(this);
        mBtService = new BluetoothService(mAdminDeviceMessageHandler, BluetoothAdapter.getDefaultAdapter());
    }

    private void processPacket(ControlPacket controlPacket) {
        Command command = controlPacket.getCommand();
        Object[] arguments = controlPacket.getArguments();
        Log.i(TAG, "received command " + command);

        VideoActivity videoActivity = activityRef.get();
        if (videoActivity == null)
            return;

        if (command == Command.LIST) {
            VideosPacket videosPacket = new VideosPacket();
            videosPacket.setVideoList(videoActivity.getVideoList());
            reply(videosPacket);
        } else {
            switch (command) {
                case GET:
                    videoActivity.isPlaying();
                    break;
                case PLAY:
                    if (arguments.length > 0) {
                        String videoPath = (String) arguments[0];
                        File videoToPlay = videoActivity.getContentProvider().get(videoPath);
                        if (videoToPlay != null)
                            videoActivity.playVideoFile(videoToPlay);
                    }
                    break;
                case PAUSE:
                    videoActivity.playOrPauseVideo();
                    break;
                case NEXT:
                    videoActivity.playNext();
                    break;
                case PREV:
                    videoActivity.playPrev();
                    break;
                case REWIND:
                    arguments = controlPacket.getArguments();
                    if (arguments.length > 0) {
                        Double positionSec = (Double) arguments[0];
                        videoActivity.rewindTo(positionSec);
                    }
                    break;
                case RECONNECT_MUSE:
                    repository.connect();
                    break;
            }
            reply(dataPacket);
        }
    }

    private void reply(Packet packet) {
        byte[] packetBytes = packet.toBytes();
        mBtService.write(packetBytes);
    }

    private void processAdminDeviceState(int connectionState) {
        VideoActivity videoActivity = activityRef.get();
        videoActivity.setAdminConnectionState(connectionState);
    }
}
