package ru.spbstu.videomood.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
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
import android.text.Layout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.choosemuse.libmuse.ConnectionState;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import ru.spbstu.videomood.AdminDeviceMessageHandler;
import ru.spbstu.videomood.Const;
import ru.spbstu.videomood.ContentProvider;
import ru.spbstu.videomood.R;
import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Command;
import ru.spbstu.videomood.btservice.ControlPacket;
import ru.spbstu.videomood.btservice.DataPacket;

public class VideoActivity extends MuseActivity implements View.OnClickListener {

    public static final int betaPercentToWarning = 20;
    public static final int alphaPercentToWarning = 100-betaPercentToWarning;

    private final UI UI = new UI();

    private static final String TAG = "VideoMood:VideoActivity";

    private long alphaPercentSum;
    private long betaPercentSum;

    private final int timeArrayLength = 60*10;
    private final Queue<Long[]> percentTimeQueue = new ArrayDeque<>(timeArrayLength);

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        dataPacket.setHeadsetBatteryPercent(level);
        }
    };

    private void updateBattery() {
        UI.batteryTextView.setText(String.format("%d%%", (int)batteryValue));
        batteryStale = false;
    }

    private int alphaPct;
    private int betaPct;

    private void updateBar() {
        //TODO: move logic of calc alpha and beta percent to separate method

        BarValues barValues = new BarValues().calculate(relativeBuffer);
        long alphaPercent = barValues.getAlphaPercent();
        long betaPercent = barValues.getBetaPercent();

        if (percentTimeQueue.size() == timeArrayLength) {
            percentTimeQueue.remove();
        }

        Long[] percentArr = new Long[2];
        percentArr[Const.Rhythms.ALPHA] = alphaPercent;
        percentArr[Const.Rhythms.BETA] = betaPercent;
        percentTimeQueue.add(percentArr);

        alphaPct = Long.valueOf(alphaPercent).intValue();
        betaPct = Long.valueOf(betaPercent).intValue();

        UI.updateAlphaBar(alphaPercent);
        UI.updateBetaBar(betaPercent);
    }

    private final long second = 1000;

    private long checkCalmDelay = 10 * second;
    private long checkWarningDelay = 90 * second;
    private long checkWarningPeriod = second;
    private long checkCalmPeriod = second;


    private Handler warningHandler = new Handler();

    private Handler calmHandler = new Handler();

    private final Runnable checkWarningRunnable = new Runnable() {
        @Override
        public void run() {
            calcPercentSum();
            //check if we should interrupt video and ask to calm down
            if (checkIsWarning()) {
                switchToCalmCheck(findViewById(R.id.museInfo));
            } else {
                //or we could continue watching and counting
                warningHandler.postDelayed(this, checkWarningPeriod);
            }
        }
    };

    private final Runnable checkCalmRunnable = new Runnable() {
        @Override
        public void run() {
            switchToWarningCheck(findViewById(R.id.calmScreen));

//            calcPercentSum();
//            if (checkIsCalm()) {
//                switchToWarningCheck(findViewById(R.id.calmScreen));
//            } else {
//                calmHandler.postDelayed(this, checkCalmPeriod);
//            }
        }
    };

    public void switchToCalmCheck(View view) {
        dataPacket.setIsPanic(true);
        displayCalmScreen();
        percentTimeQueue.clear();
        warningHandler.removeCallbacks(checkWarningRunnable);

        calmHandler.postDelayed(checkCalmRunnable, checkCalmDelay);
    }

    public void switchToWarningCheck(View view) {
        dataPacket.setIsPanic(false);
        hideCalmScreen();
        percentTimeQueue.clear();
        calmHandler.removeCallbacks(checkCalmRunnable);
        warningHandler.postDelayed(checkWarningRunnable, checkWarningDelay);
    }

    private boolean checkIsWarning() {
        Log.i(TAG, String.format("warning check: (%d/%d), queue size is %d", alphaPercentSum, betaPercentSum, percentTimeQueue.size()));

        return betaPercentSum >= betaPercentToWarning;
    }

    private boolean checkIsCalm() {
        Log.i(TAG, String.format("calm check: (%d/%d), queue size is %d", alphaPercentSum, betaPercentSum, percentTimeQueue.size()));

        return alphaPercentSum >= alphaPercentToWarning;
    }

    private void calcPercentSum() {
        long inAlphaCount = 0;
        long inBetaCount = 0;
        for (Long[] percentArr : percentTimeQueue) {
            if (percentArr[Const.Rhythms.BETA] > percentArr[Const.Rhythms.ALPHA])
                inBetaCount++;
            else
                inAlphaCount++;
        }

        int countSum = percentTimeQueue.size();
        alphaPercentSum = (long)( 100.0 * (double)inAlphaCount / countSum ) ;
        betaPercentSum = (long)( 100.0 * (double)inBetaCount / countSum ) ;
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
            if (relativeStale) {
                updateBar();
                relativeStale = false;
            }
            if (sensorsStale) {
                UI.updateMuseSensors(sensorsStateBuffer, isForeheadTouch);
                dataPacket.setMuseSensorsState(sensorsStateBuffer);
                sensorsStale = false;
            }
            if (batteryStale) {
                updateBattery();
                dataPacket.setMuseBatteryPercent((int)batteryValue);
                batteryStale = false;
            }
            if (isConnectionStatusStale) {
                updateConnectionStatus();
            }
            if (shouldHideSidebar) {
                VideoActivity.this.UI.hideMuseInfo();
                shouldHideSidebar = false;
            }
            handler.postDelayed(tickUi, 1000 / 60);
        }
    };

    private void updateConnectionStatus() {
        ConnectionState connectionState = getConnectionState();
        switch (connectionState) {
            case CONNECTING:
                processConnecting();
                break;
            case CONNECTED:
                processConnect();
                break;
            case DISCONNECTED:
                processDisconnect();
                break;
        }
        isConnectionStatusStale = false;
    }

    private ContentProvider contentProvider;

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private Handler mAdminDeviceMessageHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_video);

        UI.setup();

        try {
            contentProvider = new ContentProvider();
            File videoFile = contentProvider.getNext();
            currentVideoUri = Uri.fromFile(videoFile);
            dataPacket.setVideoName(videoFile.getName());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            displayErrorDialog();
            return;
        }

        mAdminDeviceMessageHandler = new AdminDeviceMessageHandler(new WeakReference<>(this));

        registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Start our asynchronous updates of the UI.
        handler.post(tickUi);

        View view = findViewById(R.id.videoActivity);
        view.setOnClickListener(VideoActivity.this);

        sidebarVisibilityTimer.schedule(sidebarVisibilityTimerTask, 5*second);
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
        this.UI.showMuseInfo();

        //start timer to hide sidebar
        sidebarVisibilityTimer.cancel();

        sidebarVisibilityTimer = new Timer();
        sidebarVisibilityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                shouldHideSidebar = true;
            }
        }, 5 * second);

        VideoActivity.this.UI.mediaController.show();

        //return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mBtService == null) {
            setupBtService();
        }
    }

    /**
     * Member object for the chat services
     */
    private BluetoothService mBtService = null;

    private void setupBtService() {
        mBtService = new BluetoothService(mAdminDeviceMessageHandler, BluetoothAdapter.getDefaultAdapter());
    }

    private final DataPacket dataPacket = new DataPacket();

    public void processAdminDevicePacket(ControlPacket controlPacket) {
        Command command = controlPacket.getCommand();
        Object[] arguments = controlPacket.getArguments();
        Log.i(TAG, "received command " + command);
        switch (command) {
            case GET:
                dataPacket.setAlphaPct(alphaPct);
                dataPacket.setBetaPct(betaPct);
                dataPacket.setMuseSensorsState(sensorsStateBuffer);
                dataPacket.setVideoState(UI.videoView.isPlaying());
                dataPacket.setDurationSec(UI.videoView.getDuration() / 1000);
                dataPacket.setCurrentPositionSec(UI.videoView.getCurrentPosition() / 1000);
                break;
            case LIST:
                dataPacket.setVideoList(contentProvider.getContentList());
                break;
            case PLAY:
                if (arguments.length > 0) {
                    String videoPath = (String)arguments[0];
                    File videoToPlay = contentProvider.get(videoPath);
                    if (videoToPlay != null)
                        playVideoFile(videoToPlay);
                }
                break;
            case PAUSE:
                if (UI.videoView.isPlaying())
                    pauseVideo();
                else
                    playVideo();
                break;
            case NEXT:
                File nextVideo = contentProvider.getNext();
                playVideoFile(nextVideo);
                break;
            case PREV:
                File prevVideo = contentProvider.getPrev();
                playVideoFile(prevVideo);
                break;
            case REWIND:
                arguments = controlPacket.getArguments();
                if (arguments.length > 0)
                {
                    Double positionPct = (Double) arguments[0];
                    UI.videoView.seekTo((int) (positionPct * 1000));
                }
                break;
            case RECONNECT_MUSE:
                connectMuse();
                break;
        }
        reply();
        dataPacket.setAlphaPct(null);
        dataPacket.setBetaPct(null);
    }

    private void reply() {
        byte[] packetBytes = dataPacket.toBytes();
        mBtService.write(packetBytes);
        dataPacket.setVideoList(null);
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

    private void playVideo(){
        UI.videoView.start();
        dataPacket.setVideoState(true);
    }

    private void pauseVideo(){
        UI.videoView.pause();
        dataPacket.setVideoState(false);
    }

    private void setAdminDeviceStatus(int stringResId) {
        UI.adminDeviceConnectionStatusTextView.setText(stringResId);
    }

    //todo: add exact reason
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
                        connectMuse();
                        UI.videoView.start();
                    }
                })
                .show();
    }

    public void processConnecting() {
        UI.museState.setText(R.string.state_connecting);
    }

    public void processConnect() {
        UI.processMuseConnect();
        dataPacket.setMuseState(true);
        warningHandler.postDelayed(checkWarningRunnable, 60 * second);
    }

    public void processDisconnect() {
        UI.museState.setText(R.string.state_disconnected);
        UI.setMuseIndicatorsVisible(false);
        dataPacket.setMuseState(false);
        warningHandler.removeCallbacks(checkWarningRunnable);
        calmHandler.removeCallbacks(checkCalmRunnable);
        UI.videoView.pause();

        if (adminConnectionState != BluetoothService.STATE_CONNECTED)
            displayReconnectDialog();
    }

    private Uri currentVideoUri;

    private void playVideoFile(File file) {
        dataPacket.setVideoName(file.getName());
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

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBtService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBtService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth service
                mBtService.startServer();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtService != null) {
            mBtService.stop();
        }

        unregisterReceiver(receiver);
    }

    private int adminConnectionState = BluetoothService.STATE_NONE;

    public void processAdminDeviceState(int connectionState) {
        adminConnectionState = connectionState;
        switch (connectionState) {
            case BluetoothService.STATE_CONNECTED:
                setAdminDeviceStatus(R.string.state_connected);
                break;
            case BluetoothService.STATE_CONNECTING:
                setAdminDeviceStatus(R.string.state_connecting);
                break;
            case BluetoothService.STATE_LISTEN:
            case BluetoothService.STATE_NONE:
                setAdminDeviceStatus(R.string.state_disconnected);
                break;
        }
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

        void setMuseIndicatorsVisible(boolean isVisible) {
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
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
                        VideoActivity.this.onBackPressed();

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
                            dataPacket.setCurrentPositionSec(mp.getCurrentPosition() / 1000);
                            dataPacket.setDurationSec(mp.getDuration());
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
        }

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

        void updateAlphaBar(float alphaPercent) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) alphaBar.getLayoutParams();
            params.weight = alphaPercent;
            alphaBar.setLayoutParams(params);
        }

        void updateBetaBar(float betaPercent) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) betaBar.getLayoutParams();
            params.weight = betaPercent;
            betaBar.setLayoutParams(params);
        }
    }
}

