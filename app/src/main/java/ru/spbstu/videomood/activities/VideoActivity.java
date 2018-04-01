package ru.spbstu.videomood.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.choosemuse.libmuse.ConnectionState;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import ru.spbstu.videomood.AdminDeviceManager;
import ru.spbstu.videomood.Const;
import ru.spbstu.videomood.ContentProvider;
import ru.spbstu.videomood.MuseData;
import ru.spbstu.videomood.MuseDataRepository;
import ru.spbstu.videomood.MuseDataViewModel;
import ru.spbstu.videomood.R;
import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.VideoActivityState;
import ru.spbstu.videomood.btservice.MuseState;

public class VideoActivity extends AppCompatActivity implements View.OnClickListener {

    private final UI UI = new UI();

    private static final String TAG = "VideoMood:VideoActivity";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            videoActivityState.setHeadsetBatteryPercent(level);
            postLiveData();
        }
    };
    private MuseData museData;
    private boolean museDataStale = false;

    private void updateBattery(Integer batteryValue) {
        if (batteryValue != null)
            UI.batteryTextView.setText(String.format("%d%%", batteryValue));
    }

    private void updateBar(Integer alphaPercent, Integer betaPercent) {
        UI.updateAlphaBar(alphaPercent);
        UI.updateBetaBar(betaPercent);
    }

    public void switchToPanicMode() {
        videoActivityState.setIsPanic(true);
        postLiveData();
        displayCalmScreen();
    }

    public void switchToNormalMode() {
        videoActivityState.setIsPanic(false);
        postLiveData();
        hideCalmScreen();
    }

    /**
     * We will be updating the UI using a handler instead of in packet handlers because
     * packets come in at a very high frequency and it only makes sense to update the UI
     * at about 60fps. The update functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Handler handler = new Handler();

    /**
     * The runnable that is used to update the UI at 60Hz.
     * <p>
     * We update the UI from this Runnable instead of in packet handlers
     * because packets come in at high frequency -- 220Hz or more for raw EEG
     * -- and it only makes sense to update the UI at about 60fps. The update
     * functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            if (museDataStale) {
                updateMuseConnectionStatus(museData.connectionState);
                updateBar(museData.alphaPercent, museData.betaPercent);
                updateBattery(museData.batteryPercent);
                updateMode(museData.isPanic);
                updateSensors(museData.sensorsStateBuffer, museData.isForeheadTouch);
                museDataStale = false;
            }
            if (shouldHideSidebar) {
                VideoActivity.this.UI.hideMuseInfo();
                shouldHideSidebar = false;
            }
            handler.postDelayed(tickUi, 1000 / 60);
        }
    };

    private void updateSensors(boolean[] sensorsStateBuffer, Boolean isForeheadTouch) {
        if (isForeheadTouch == null || sensorsStateBuffer == null)
            return;

        UI.updateMuseSensors(sensorsStateBuffer, isForeheadTouch);
        videoActivityState.setMuseSensorsState(sensorsStateBuffer);
        postLiveData();
    }

    public void updateMuseConnectionStatus(ConnectionState connectionState) {
        switch (connectionState) {
            case CONNECTING:
                onMuseConnecting();
                break;
            case CONNECTED:
                onMuseConnect();
                break;
            case DISCONNECTED:
                onMuseDisconnect();
                break;
        }
    }

    private ContentProvider contentProvider;

    public ContentProvider getContentProvider() {
        return contentProvider;
    }

    private MuseDataRepository repository;

    private boolean isPanic = false;

    private MutableLiveData<VideoActivityState> liveData = new MutableLiveData<>();

    private VideoActivityState videoActivityState = new VideoActivityState() ;

    private void postLiveData() {
        liveData.setValue(videoActivityState);
    }

    public LiveData<VideoActivityState> getVideoActivityState(){
        return liveData;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AdminDeviceManager adminDeviceManager = new AdminDeviceManager(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_video);

        UI.setup();

        try {
            contentProvider = new ContentProvider();
            File videoFile = contentProvider.getNext();
            currentVideoUri = Uri.fromFile(videoFile);
            videoActivityState.setVideoName(videoFile.getName());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            displayErrorDialog();
            return;
        }

        registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        View view = findViewById(R.id.videoActivity);
        view.setOnClickListener(VideoActivity.this);
    }

    private MuseDataViewModel museDataVM;

    @Override
    protected void onStart() {
        super.onStart();

        repository = new MuseDataRepository(this);
        //museDataVM = new MuseDataViewModel(repository);
        repository.getMuseData().observe(this, new Observer<MuseData>() {
            @Override
            public void onChanged(@Nullable MuseData data) {
                if (data != null) {
                    museData = data;
                    museDataStale = true;
                }
            }
        });

        // Start our asynchronous updates of the UI.
        handler.post(tickUi);

        sidebarVisibilityTimer.schedule(sidebarVisibilityTimerTask, 5*Const.SECOND);
    }

    private void updateMode(Boolean newIsPanic) {
        if (newIsPanic == null)
            return;

        if (!isPanic && newIsPanic) {
            switchToPanicMode();
        } else if (isPanic && !newIsPanic) {
            switchToNormalMode();
        }
        isPanic = newIsPanic;
    }

    private boolean shouldHideSidebar = false;

    private Timer sidebarVisibilityTimer = new Timer();

    private TimerTask sidebarVisibilityTimerTask = new TimerTask() {
        @Override
        public void run() {
            shouldHideSidebar = true;
        }
    };

    @Override
    public void onClick(View v) {
        //show sidebar
        UI.showMuseInfo();

        //start timer to hide sidebar
        sidebarVisibilityTimer.cancel();

        sidebarVisibilityTimer = new Timer();
        sidebarVisibilityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                shouldHideSidebar = true;
            }
        }, 5 * Const.SECOND);

        UI.mediaController.show();
    }

    private void displayCalmScreen() {
        UI.calmScreen.setVisibility(View.VISIBLE);
        pauseVideo();
        UI.mediaController.hide();
    }

    private void hideCalmScreen() {
        UI.calmScreen.setVisibility(View.INVISIBLE);
        playVideo();
    }

    public void playOrPauseVideo() {
        if (isPlaying())
            pauseVideo();
        else
            playVideo();
    }

    public void playVideo(){
        UI.videoView.start();
        videoActivityState.setVideoState(true);
        postLiveData();
    }

    public void pauseVideo(){
        UI.videoView.pause();
        videoActivityState.setVideoState(false);
        postLiveData();
    }

    private int adminConnectionState = BluetoothService.STATE_NONE;

    private @StringRes int getStateStringResId(int adminConnectionState) {
        switch (adminConnectionState) {
            case BluetoothService.STATE_CONNECTED:
                return R.string.state_connected;
            case BluetoothService.STATE_CONNECTING:
                return R.string.state_connecting;
            case BluetoothService.STATE_LISTEN:
            case BluetoothService.STATE_NONE:
            default:
                return R.string.state_disconnected;
        }
    }

    public void setAdminConnectionState(int adminConnectionState) {
        this.adminConnectionState = adminConnectionState;
        int statusStringResId = getStateStringResId(adminConnectionState);
        setAdminDeviceStatusTv(statusStringResId);
    }

    public void setAdminDeviceStatusTv(@StringRes int stringResId) {
        UI.adminDeviceConnectionStatusTextView.setText(stringResId);
    }

    private void displayErrorDialog() {
        final Activity activity = this;
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.error_no_videofiles)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        activity.finishAffinity();
                    }
                })
                .show();
    }

    private void displayReconnectDialog() {
        Resources resources = getResources();
        new AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.reconnect_header))
                .setMessage(resources.getString(R.string.reconnect_message))
                .setNegativeButton(resources.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        UI.videoView.start();
                    }
                })
                .setPositiveButton(resources.getString(R.string.reconnect), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        repository.connect();
                        UI.videoView.start();
                    }
                })
                .show();
    }

    public void onMuseConnecting() {
        UI.museState.setText(R.string.state_connecting);
        videoActivityState.setMuseState(MuseState.CONNECTING);
        postLiveData();
    }

    public void onMuseConnect() {
        UI.processMuseConnect();
        videoActivityState.setMuseState(MuseState.CONNECTED);
        postLiveData();
    }

    public void onMuseDisconnect() {
        UI.museState.setText(R.string.state_disconnected);
        UI.setMuseIndicatorsVisible(false);
        videoActivityState.setMuseState(MuseState.DISCONNECTED);
        postLiveData();

        if (adminConnectionState != BluetoothService.STATE_CONNECTED)
        {
            UI.videoView.pause();
            displayReconnectDialog();
        }
    }

    private Uri currentVideoUri;

    public void playVideoFile(File file) {
        videoActivityState.setVideoName(file.getName());
        postLiveData();
        currentVideoUri = Uri.fromFile(file);
        UI.videoView.setVideoURI(currentVideoUri);
        playVideo();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private int currentPlayPosition = -1;

    public static final String CURRENT_VIDEO_URI_KEY = "currentVideoUri";
    public static final String CURRENT_VIDEO_POSITION_KEY = "currentVideoPosition";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_VIDEO_POSITION_KEY, currentPlayPosition);
        outState.putParcelable(CURRENT_VIDEO_URI_KEY, currentVideoUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentPlayPosition = savedInstanceState.getInt(CURRENT_VIDEO_POSITION_KEY, -1);
        Uri saved = savedInstanceState.getParcelable(CURRENT_VIDEO_URI_KEY);
        if (saved != null)
            currentVideoUri = saved;
    }

    @Override
    protected void onPause() {
        super.onPause();

        pauseVideo();
        currentPlayPosition = UI.videoView.getCurrentPosition();
    }

    @Override
    protected void onResume() {
        super.onResume();

        UI.videoView.setVideoURI(currentVideoUri);
        if (currentPlayPosition != -1) {
            UI.videoView.seekTo(currentPlayPosition);
            pauseVideo();
        }
    }

    public int getDurationSec() {
        return UI.videoView.getDuration() / 1000;
    }

    public boolean isPlaying() {
        return UI.videoView.isPlaying();
    }

    public int getCurrentPositionSec() {
        return UI.videoView.getCurrentPosition() / 1000;
    }

    public void playNext() {
        File nextVideo = contentProvider.getNext();
        playVideoFile(nextVideo);
    }

    public void playPrev() {
        File prevVideo = contentProvider.getPrev();
        playVideoFile(prevVideo);
    }

    public void rewindTo(double positionSec) {
        UI.videoView.seekTo((int) (positionSec * 1000));
    }

    public void setVideoList() {
        videoActivityState.setVideoList(contentProvider.getContentList());
        postLiveData();
    }

    public void clearVideoList() {
        videoActivityState.setVideoList(null);
        postLiveData();
    }

    private final class UI {
        LinearLayout museInfo;

        TextView foreheadTouch;

        TextView[] isGoodIndicators;

        TextView batteryTextView;

        TextView adminDeviceConnectionStatusTextView;

        VideoView videoView;

        MediaController mediaController;

        TextView museState;

        LinearLayout museIndicators;

        void hideMuseInfo() {
            museInfo.setVisibility(View.INVISIBLE);
        }

        void showMuseInfo() {
            museInfo.setVisibility(View.VISIBLE);
        }

        private void updateMuseSensors(boolean[] sensorsStateBuffer, boolean isForeheadTouch) {
            for (int i = 0; i < sensorsStateBuffer.length; i++)
                isGoodIndicators[i].setVisibility(sensorsStateBuffer[i] ? View.VISIBLE : View.INVISIBLE);

            int visibility = isForeheadTouch ? View.VISIBLE : View.INVISIBLE;

            foreheadTouch.setVisibility(visibility);
            rhythmsBar.setVisibility(visibility);
        }

        void setMuseIndicatorsVisible(Boolean isVisible) {
            int visibility = isVisible ? View.VISIBLE : View.INVISIBLE;
            UI.museIndicators.setVisibility(visibility);
        }

        RelativeLayout calmScreen;
        LinearLayout rhythmsBar;
        TextView alphaBar;
        TextView betaBar;

        private void initTextViews(){
            UI.alphaBar = (TextView) findViewById(R.id.alpha);
            UI.betaBar = (TextView) findViewById(R.id.beta);
            UI.batteryTextView = (TextView) findViewById(R.id.battery);
            UI.museState = (TextView) findViewById(R.id.museState);

            UI.isGoodIndicators = new TextView[4];
            UI.isGoodIndicators[Const.Electrodes.FIRST] = (TextView) findViewById(R.id.good1);
            UI.isGoodIndicators[Const.Electrodes.SECOND] = (TextView) findViewById(R.id.good2);
            UI.isGoodIndicators[Const.Electrodes.THIRD] = (TextView) findViewById(R.id.good3);
            UI.isGoodIndicators[Const.Electrodes.FOURTH] = (TextView) findViewById(R.id.good4);

            UI.foreheadTouch = (TextView) findViewById(R.id.forehead);

            UI.museIndicators = (LinearLayout) findViewById(R.id.museIndicators);

            UI.adminDeviceConnectionStatusTextView = (TextView) findViewById(R.id.connectionStatus);
        }

        private void initVideoView() {
            UI.mediaController = new MediaController(VideoActivity.this){
                @Override
                public boolean dispatchKeyEvent(KeyEvent event)
                {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        if (event.getAction() == KeyEvent.ACTION_UP)
                            ((Activity) getContext()).finish();
                    else if (event.getAction() == KeyEvent.ACTION_DOWN)
                        return false;

                    return super.dispatchKeyEvent(event);
                }
            };
            UI.videoView = (VideoView) findViewById(R.id.videoView);
            UI.videoView.setMediaController(UI.mediaController);
            UI.videoView.requestFocus();
            UI.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    File nextVideo = contentProvider.getNext();
                    playVideoFile(nextVideo);
                }
            });
            UI.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                        @Override
                        public void onSeekComplete(MediaPlayer mp) {
                            videoActivityState.setCurrentPositionSec(mp.getCurrentPosition() / 1000);
                            videoActivityState.setDurationSec(mp.getDuration() / 1000);
                            postLiveData();
                        }
                    });
                    UI.videoView.start();
                }
            });
            UI.videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    File nextVideo = contentProvider.getNext();
                    playVideoFile(nextVideo);
                    return true;
                }
            });
            UI.mediaController.setPrevNextListeners(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            File prevVideo = contentProvider.getPrev();
                            playVideoFile(prevVideo);
                        }
                    },
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            File nextVideo = contentProvider.getNext();
                            playVideoFile(nextVideo);
                        }
                    });
            Runnable videoPositionTracker = new Runnable() {
                @Override
                public void run() {
                    videoActivityState.setCurrentPositionSec(getCurrentPositionSec());
                    postLiveData();
                    videoPositionTrackerHandler.postDelayed(this, Const.SECOND);
                }
            };
            videoPositionTrackerHandler.postDelayed(videoPositionTracker, Const.SECOND);
        }

        private Handler videoPositionTrackerHandler = new Handler();

        void setup() {
            initTextViews();
            initVideoView();

            calmScreen = (RelativeLayout) findViewById(R.id.calmScreen);
            rhythmsBar = (LinearLayout) findViewById(R.id.rhythmsBar);
            museInfo = (LinearLayout) findViewById(R.id.museInfo);
        }

        void processMuseConnect() {
            museState.setText(R.string.state_connected);
            setMuseIndicatorsVisible(true);
        }

        void updateAlphaBar(Integer alphaPercent) {
            if (alphaPercent == null)
                return;

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) alphaBar.getLayoutParams();
            params.weight = alphaPercent;
            alphaBar.setLayoutParams(params);
        }

        void updateBetaBar(Integer betaPercent) {
            if (betaPercent == null)
                return;

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) betaBar.getLayoutParams();
            params.weight = betaPercent;
            betaBar.setLayoutParams(params);
        }
    }
}

