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
import ru.spbstu.videomood.btservice.VideoActivityState;

public class AdminDeviceManager implements LifecycleObserver  {
    private WeakReference<VideoActivity> activityRef;

    private VideoActivityState videoActivityState;

    public AdminDeviceManager(VideoActivity videoActivity) {
        activityRef = new WeakReference<>(videoActivity);
        repository = new MuseDataRepository(videoActivity);
        videoActivity.getVideoActivityState().observe(videoActivity, new Observer<VideoActivityState>() {
            @Override
            public void onChanged(@Nullable VideoActivityState videoActivityState) {
                AdminDeviceManager.this.videoActivityState = videoActivityState;
            }
        });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onStart() {
        if (mBtService == null) {
            setupBtService();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void onDestroy() {
        if (mBtService != null) {
            mBtService.stop();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void onResume() {
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
                        ControlPacket cp = ControlPacket.createFrom((String) msg.obj);
                        if (cp != null)
                            processAdminDevicePacket(cp);
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

    public void processAdminDevicePacket(ControlPacket controlPacket) {
        Command command = controlPacket.getCommand();
        Object[] arguments = controlPacket.getArguments();
        Log.i(TAG, "received command " + command);

        VideoActivity videoActivity = activityRef.get();
        switch (command) {
            case GET:
                //we want send videoActivityState
                //so do nothing and videoActivityState will be packed and sent
                break;
            case LIST:
                videoActivity.setVideoList();
                break;
            case PLAY:
                if (arguments.length > 0) {
                    String videoPath = (String)arguments[0];
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
                if (arguments.length > 0)
                {
                    Double positionSec = (Double) arguments[0];
                    videoActivity.rewindTo(positionSec);
                }
                break;
            case RECONNECT_MUSE:

                break;
        }
        reply();
        videoActivityState.setAlphaPct(null);
        videoActivityState.setBetaPct(null);
        videoActivity.clearVideoList();
    }

    private MuseDataRepository repository;

    private void reply() {
        byte[] packetBytes = videoActivityState.toBytes();
        mBtService.write(packetBytes);
        videoActivityState.setVideoList(null);
    }

    public void processAdminDeviceState(int connectionState) {
        VideoActivity videoActivity = activityRef.get();
        videoActivity.setAdminConnectionState(connectionState);
    }

    public void videoFileChanged(String name) {
        videoActivityState.setVideoName(name);
    }
}
