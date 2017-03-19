package ru.spbstu.videomood.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.choosemuse.libmuse.ConnectionState;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Queue;

import ru.spbstu.videomood.AdminDeviceMessageHandler;
import ru.spbstu.videomood.Const;
import ru.spbstu.videomood.ContentProvider;
import ru.spbstu.videomood.R;
import ru.spbstu.videomood.User;
import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Command;
import ru.spbstu.videomood.btservice.ControlPacket;
import ru.spbstu.videomood.btservice.DataPacket;

public class VideoActivity extends MuseActivity {
    private static final String TAG = "VideoMood:VideoActivity";

    private long alphaPercentSum;
    private long betaPercentSum;

    private final int timeArrayLength = 60*10;
    private final Queue<Long[]> percentTimeQueue = new ArrayDeque<>(timeArrayLength);

    private TextView foreheadTouch;

    private TextView[] isGoodIndicators;

    private TextView batteryTextView;

    private TextView adminDeviceConnectionStatus;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        dataPacket.setHeadsetBatteryPercent(level);
        }
    };

    private void updateMuseSensors() {
        for (int i = 0; i < sensorsStateBuffer.length; i++)
            isGoodIndicators[i].setVisibility(sensorsStateBuffer[i] ? View.VISIBLE : View.INVISIBLE);

        int visibility = isForeheadTouch ? View.VISIBLE : View.INVISIBLE;

        foreheadTouch.setVisibility(visibility);
        rhythmsBar.setVisibility(visibility);
    }

    private void updateBattery() {
        batteryTextView.setText(String.format("%d%%", (int)batteryValue));
        batteryStale = false;
    }

    private LinearLayout rhythmsBar;
    private TextView alphaBar;
    private TextView betaBar;

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

        alphaPct = new Long(alphaPercent).intValue();
        betaPct = new Long(betaPercent).intValue();

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) alphaBar.getLayoutParams();
        params.weight = (float)alphaPercent;
        alphaBar.setLayoutParams(params);

        params = (LinearLayout.LayoutParams) betaBar.getLayoutParams();
        params.weight = (float)betaPercent;
        betaBar.setLayoutParams(params);
    }

    private final long second = 1000;

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
                long checkWarningDelay = second;
                warningHandler.postDelayed(this, checkWarningDelay);
            }
        }
    };

    private final Runnable checkCalmRunnable = new Runnable() {
        @Override
        public void run() {
            calcPercentSum();
            if (checkIsCalm()) {
                switchToWarningCheck(findViewById(R.id.calmScreen));
            } else {
                long checkCalmDelay = second;
                calmHandler.postDelayed(this, checkCalmDelay);
            }
        }
    };

    public void switchToCalmCheck(View view) {
        dataPacket.setIsPanic(true);
        displayCalmScreen();
        percentTimeQueue.clear();
        warningHandler.removeCallbacks(checkWarningRunnable);
        calmHandler.postDelayed(checkCalmRunnable, 10 * second);
    }

    public void switchToWarningCheck(View view) {
        dataPacket.setIsPanic(false);
        hideCalmScreen();
        percentTimeQueue.clear();
        calmHandler.removeCallbacks(checkCalmRunnable);
        warningHandler.postDelayed(checkWarningRunnable, 20 * second);
    }

    private boolean checkIsWarning() {
        Log.i(TAG, String.format("warning check: (%d/%d), queue size is %d", alphaPercentSum, betaPercentSum, percentTimeQueue.size()));

        return betaPercentSum >= 20;
    }

    private boolean checkIsCalm() {
        Log.i(TAG, String.format("calm check: (%d/%d), queue size is %d", alphaPercentSum, betaPercentSum, percentTimeQueue.size()));

        return alphaPercentSum >= 90;
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
                updateMuseSensors();
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

    private Bitmap takeScreenshot() {
        View view = getWindow().getDecorView().getRootView();
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private ContentProvider contentProvider;

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private Handler mAdminDeviceMessageHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUI();

        try {
            contentProvider = new ContentProvider(User.getAgeRangeIndex());
            File videoFile = contentProvider.getNext();
            currentVideoUri = Uri.fromFile(videoFile);
            dataPacket.setVideoName(videoFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog();
            return;
        }

        mAdminDeviceMessageHandler = new AdminDeviceMessageHandler(new WeakReference<>(this));

        registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Start our asynchronous updates of the UI.
        handler.post(tickUi);
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
        mBtService = new BluetoothService(this, mAdminDeviceMessageHandler);
    }

    private final DataPacket dataPacket = new DataPacket();

    public void processAdminDevicePacket(ControlPacket controlPacket) {
        Command command = controlPacket.getCommand();
        Log.i(TAG, "received command " + command);
        switch (command) {
            case GET:
                dataPacket.setAlphaPct(alphaPct);
                dataPacket.setBetaPct(betaPct);
                writeScreenshotTo(dataPacket);
                break;
            case LIST:
                dataPacket.setVideoList(contentProvider.getContentList());
                break;
            case PLAY:
                Object[] arguments = controlPacket.getArguments();
                if (arguments.length > 0) {
                    Integer videoIndex = ((Double) arguments[0]).intValue();
                    File videoToPlay = contentProvider.get(videoIndex);
                    playVideoFile(videoToPlay);
                }
                break;
            case PAUSE:
                if (videoView.isPlaying())
                    videoView.pause();
                else
                    videoView.start();
                dataPacket.setVideoState(videoView.isPlaying());
                break;
            case NEXT:
                File nextVideo = contentProvider.getNext();
                playVideoFile(nextVideo);
                break;
            case PREV:
                File prevVideo = contentProvider.getPrev();
                playVideoFile(prevVideo);
                break;
        }
        reply();
        dataPacket.setAlphaPct(null);
        dataPacket.setBetaPct(null);
    }

    private static final int screenshotWidth = 100;
    private static final int screenshotHeight = 100;
    private static final int compressQuality = 0;

    private void writeScreenshotTo(DataPacket dataPacket) {
        Bitmap screenshot = takeScreenshot();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(screenshot, screenshotWidth, screenshotHeight, false);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, out);
            dataPacket.setScreenshot(out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reply() {
        byte[] packetBytes = dataPacket.toBytes();
        mBtService.write(packetBytes);
        dataPacket.setVideoList(null);
    }

    private RelativeLayout calmScreen;

    private void displayCalmScreen() {
        videoView.pause();
        dataPacket.setVideoState(false);
        mediaController.hide();
        calmScreen.setVisibility(View.VISIBLE);
    }

    private void hideCalmScreen() {
        calmScreen.setVisibility(View.INVISIBLE);
        videoView.start();
        dataPacket.setVideoState(true);
    }

    private void setupUI() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_video);
        initTextViews();
        initVideoView();

        calmScreen = (RelativeLayout) findViewById(R.id.calmScreen);
        rhythmsBar = (LinearLayout) findViewById(R.id.rhythmsBar);
    }

    public void setAdminDeviceStatus(int stringResId) {
        adminDeviceConnectionStatus.setText(stringResId);
    }

    private void initTextViews(){
        alphaBar = (TextView) findViewById(R.id.alpha);
        betaBar = (TextView) findViewById(R.id.beta);
        batteryTextView = (TextView) findViewById(R.id.battery);
        museState = (TextView) findViewById(R.id.museState);

        isGoodIndicators = new TextView[4];
        isGoodIndicators[Const.Electrodes.FIRST] = (TextView) findViewById(R.id.good1);
        isGoodIndicators[Const.Electrodes.SECOND] = (TextView) findViewById(R.id.good2);
        isGoodIndicators[Const.Electrodes.THIRD] = (TextView) findViewById(R.id.good3);
        isGoodIndicators[Const.Electrodes.FOURTH] = (TextView) findViewById(R.id.good4);

        foreheadTouch = (TextView) findViewById(R.id.forehead);

        museIndicators = (LinearLayout) findViewById(R.id.museIndicators);

        adminDeviceConnectionStatus = (TextView) findViewById(R.id.connectionStatus);
    }

    //todo: add exact reason
    private void displayErrorDialog() {
        final Activity activity = this;
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("No suitable video files found. Application will quit")
                .setPositiveButton("Okay", new DialogInterface.OnClickListener()
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
                    }
                })
                .setPositiveButton(resources.getString(R.string.reconnect), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        reconnectMuse();
                    }
                })
                .show();
    }

    private TextView museState;

    private LinearLayout museIndicators;

    private void setMuseIndicatorsVisible(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.INVISIBLE;
        museIndicators.setVisibility(visibility);
    }

    public void processConnecting() {
        museState.setText(R.string.state_connecting);
    }

    public void processConnect() {
        museState.setText(R.string.state_connected);
        setMuseIndicatorsVisible(true);
        dataPacket.setMuseState(true);
        warningHandler.postDelayed(checkWarningRunnable, 60 * second);
    }

    public void processDisconnect() {
        museState.setText(R.string.state_disconnected);
        setMuseIndicatorsVisible(false);
        dataPacket.setMuseState(false);
        warningHandler.removeCallbacks(checkWarningRunnable);
        calmHandler.removeCallbacks(checkCalmRunnable);
        displayReconnectDialog();
    }

    private VideoView videoView;
    private MediaController mediaController;
    private Uri currentVideoUri;

    private void initVideoView() {
        videoView = (VideoView) findViewById(R.id.videoView);
        mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        videoView.requestFocus();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                File nextVideo = contentProvider.getNext();
                playVideoFile(nextVideo);
            }
        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.start();
            }
        });
    }

    private void playVideoFile(File file) {
        dataPacket.setVideoName(file.getName());
        dataPacket.setVideoState(true);
        currentVideoUri = Uri.fromFile(file);
        videoView.setVideoURI(currentVideoUri);
        videoView.start();
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

        videoView.pause();
        dataPacket.setVideoState(false);
        currentPlayPosition = videoView.getCurrentPosition();
    }

    @Override
    protected void onResume() {
        super.onResume();

        videoView.setVideoURI(currentVideoUri);
        if (currentPlayPosition != -1) {
            videoView.seekTo(currentPlayPosition);
            videoView.pause();
            dataPacket.setVideoState(false);
        }

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBtService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBtService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBtService.start();
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
}

