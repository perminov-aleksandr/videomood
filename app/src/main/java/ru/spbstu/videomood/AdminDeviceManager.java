package ru.spbstu.videomood;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
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
    private WeakReference<VideoActivity> activityRef;

    private DataPacket dataPacket;

    private MuseDataRepository repository;

    public AdminDeviceManager(VideoActivity videoActivity) {
        activityRef = new WeakReference<>(videoActivity);
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

        repository = MuseDataRepository.getInstance(videoActivity);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void onResume() {
        Log.d(TAG, "resume event occurred");
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBtService != null) {
            // Only if the connectionState is STATE_NONE, do we know that we haven't started already
            if (mBtService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth service
                mBtService.startServer();
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void onDestroy() {
        Log.d(TAG, "destroy event occurred");
        if (mBtService != null) {
            mBtService.stop();
        }
    }

    private static final String TAG = "VideoMood:AdminManager";

    private BluetoothService mBtService = null;

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private Handler mAdminDeviceMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    processAdminDeviceState(msg.arg1);
                    break;
                case Constants.MESSAGE_PACKET:
                    try {
                        ControlPacket p = (ControlPacket) Packet.createFrom((String) msg.obj);
                        if (p != null)
                            processPacket(p);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };

    private void setupBtService() {
        mBtService = new BluetoothService(mAdminDeviceMessageHandler, BluetoothAdapter.getDefaultAdapter());
    }

    private void processPacket(ControlPacket controlPacket) {
        Command command = controlPacket.getCommand();
        Object[] arguments = controlPacket.getArguments();
        Log.i(TAG, "received command " + command);

        VideoActivity videoActivity = activityRef.get();
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
