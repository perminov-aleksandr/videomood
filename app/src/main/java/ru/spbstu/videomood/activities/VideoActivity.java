package ru.spbstu.videomood.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.os.Message;
import android.util.Log;
import android.view.View;

import android.view.Window;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.choosemuse.libmuse.Battery;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseDataPacket;
import com.google.gson.Gson;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import ru.spbstu.videomood.AdminDeviceMessageHandler;
import ru.spbstu.videomood.ConnectionListener;
import ru.spbstu.videomood.Const;
import ru.spbstu.videomood.ContentProvider;
import ru.spbstu.videomood.DataListener;
import ru.spbstu.videomood.MuseManager;
import ru.spbstu.videomood.R;
import ru.spbstu.videomood.User;
import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Command;
import ru.spbstu.videomood.btservice.Constants;
import ru.spbstu.videomood.btservice.ControlPacket;
import ru.spbstu.videomood.btservice.DataPacket;

public class VideoActivity extends Activity {

    private static final String TAG = "VideoMood:VideoActivity";

    private static final int REQUEST_ENABLE_BT = 3;

    private final int CHANNEL_COUNT = 4;
    private final int RANGE_COUNT = 5;

    private long alphaPercentSum;
    private long betaPercentSum;

    private final int timeArrayLength = 60*10;
    private final Queue<Long[]> percentTimeQueue = new ArrayDeque<>(timeArrayLength);

    private final double[][] relativeBuffer = new double[RANGE_COUNT][CHANNEL_COUNT];
    private boolean relativeStale = false;

    private TextView foreheadTouch;
    private boolean isForeheadTouch = false;

    private TextView[] isGoodIndicators;
    private final boolean[] isGoodBuffer = new boolean[CHANNEL_COUNT];
    private boolean isGoodStale = false;

    private TextView batteryTextView;
    private double batteryValue;
    private boolean batteryStale = false;
    private TextView adminDeviceConnectionStatus;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            dataPacket.setHeadsetBatteryPercent(level);
        }
    };

    private void updateIsGood() {
        for (int i = 0; i < isGoodBuffer.length; i++)
            isGoodIndicators[i].setVisibility(isGoodBuffer[i] ? View.VISIBLE : View.INVISIBLE);

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

    private void updateBar() {
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

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) alphaBar.getLayoutParams();
        params.weight = (float)alphaPercent;
        alphaBar.setLayoutParams(params);

        params = (LinearLayout.LayoutParams) betaBar.getLayoutParams();
        params.weight = (float)betaPercent;
        betaBar.setLayoutParams(params);
    }

    public void processMuseDataSensors(ArrayList<Double> packetValues) {
        Boolean[] sensorStates = new Boolean[CHANNEL_COUNT];
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            isGoodBuffer[i] = packetValues.get(i) > 0.5;
            sensorStates[i] = isGoodBuffer[i];
        }
        dataPacket.setMuseSensorsState(sensorStates);
        isGoodStale = true;
    }

    public void processMuseDataBattery(MuseDataPacket p) {
        batteryValue = p.getBatteryValue(Battery.CHARGE_PERCENTAGE_REMAINING);
        dataPacket.setMuseBatteryPercent((int)batteryValue);
        batteryStale = true;
    }

    public void processMuseDataRelative(ArrayList<Double> packetValues, int relativeIndex) {
        fillRelativeBufferWith(relativeIndex, packetValues);
        relativeStale = true;
    }

    private final long second = 1000;

    private Handler warningHandler = new Handler();

    private Handler calmHandler = new Handler();

    private final Runnable checkWarningRunnable = new Runnable() {
        @Override
        public void run() {
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
            if (checkIsCalm()) {
                switchToWarningCheck(findViewById(R.id.calmScreen));
            } else {
                long checkCalmDelay = second;
                calmHandler.postDelayed(this, checkCalmDelay);
            }
        }
    };

    public void switchToCalmCheck(View view) {
        displayCalmScreen();
        percentTimeQueue.clear();
        warningHandler.removeCallbacks(checkWarningRunnable);
        calmHandler.postDelayed(checkCalmRunnable, 30 * second);
    }

    public void switchToWarningCheck(View view) {
        hideCalmScreen();
        percentTimeQueue.clear();
        calmHandler.removeCallbacks(checkCalmRunnable);
        warningHandler.postDelayed(checkWarningRunnable, 60 * second);
    }

    private boolean checkIsWarning() {
        calcPercentSum();

        Log.i(TAG, String.format("warning check: (%d/%d), queue size is %d", alphaPercentSum, betaPercentSum, percentTimeQueue.size()));

        return betaPercentSum >= 20;
    }

    private boolean checkIsCalm() {
        calcPercentSum();

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

        dataPacket.setAlphaPct((int)alphaPercentSum);
        dataPacket.setBetaPct((int)betaPercentSum);
    }

    private void fillRelativeBufferWith(final int rangeIndex, final ArrayList<Double> packetValues) {
        for (int i = 0; i < packetValues.size(); i++) {
            Double v = packetValues.get(i);
            relativeBuffer[rangeIndex][i] = v;
        }
    }

    public void processMuseArtifactPacket(final MuseArtifactPacket p) {
        isForeheadTouch = p.getHeadbandOn();
        isGoodStale = true;
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
            if (isGoodStale) {
                updateIsGood();
                isGoodStale = false;
            }
            if (batteryStale) {
                updateBattery();
                batteryStale = false;
            }
            handler.postDelayed(tickUi, 1000 / 60);
        }
    };

    private ContentProvider contentProvider;

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private Handler mAdminDeviceMessageHandler;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUI();

        try {
            contentProvider = new ContentProvider(User.getAgeRangeIndex());
            currentVideoUri = Uri.fromFile(contentProvider.getNext());
        } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog();
            return;
        }

        WeakReference<VideoActivity> weakActivity = new WeakReference<>(this);
        // Register a listener to handle connection state changes
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);

        setupMuseManager();

        mAdminDeviceMessageHandler = new AdminDeviceMessageHandler(new WeakReference<>(this));

        registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Start our asynchronous updates of the UI.
        handler.post(tickUi);
    }

    private void setupMuseManager() {
        MuseManager.setContext(this);
        MuseManager.registerMuseListeners(connectionListener, dataListener);

        // Cache the Muse that the user has selected.
        muse = MuseManager.getMuse();
        //start receiving muse packets
        muse.runAsynchronously();
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
                reply();
                break;
            case LIST:
                dataPacket.setVideoList(contentProvider.getContentList());
                reply();
                break;
            case PLAY:
                Object[] arguments = controlPacket.getArguments();
                if (arguments.length > 0) {
                    Integer videoIndex = (Integer) arguments[0];
                    File videoToPlay = contentProvider.get(videoIndex);
                    playVideoFile(videoToPlay);
                    dataPacket.setVideoName(videoToPlay.getName());
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
                dataPacket.setVideoName(nextVideo.getName());
                break;
            case PREV:
                File prevVideo = contentProvider.getPrev();
                playVideoFile(prevVideo);
                dataPacket.setVideoName(prevVideo.getName());
                break;
        }
    }

    private void reply() {
        String serializedPacket = new Gson().toJson(dataPacket);
        mBtService.write(serializedPacket.getBytes());
    }

    private RelativeLayout calmScreen;

    private void displayCalmScreen() {
        videoView.pause();
        mediaController.hide();
        calmScreen.setVisibility(View.VISIBLE);
    }

    private void hideCalmScreen() {
        calmScreen.setVisibility(View.INVISIBLE);
        videoView.start();
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
                        muse.runAsynchronously();
                    }
                })
                .show();
    }

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     * <p>
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     * <p>
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener;

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
        dataPacket.setMuseState(true);
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
            playVideoFile(contentProvider.getNext());
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

        if (muse != null)
            muse.enableDataTransmission(false);

        videoView.pause();
        currentPlayPosition = videoView.getCurrentPosition();
    }

    @Override
    protected void onResume() {
        super.onResume();

        videoView.setVideoURI(currentVideoUri);
        if (currentPlayPosition != -1) {
            videoView.seekTo(currentPlayPosition);
        }

        if (muse != null)
            muse.enableDataTransmission(true);

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

        if (muse != null) {
            muse.unregisterAllListeners();
            muse.disconnect(false);
        }

        if (mBtService != null) {
            mBtService.stop();
        }

        unregisterReceiver(receiver);
    }
}

