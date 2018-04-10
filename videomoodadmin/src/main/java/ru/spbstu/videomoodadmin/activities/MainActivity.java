package ru.spbstu.videomoodadmin.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Command;
import ru.spbstu.videomood.btservice.Constants;
import ru.spbstu.videomood.btservice.ControlPacket;
import ru.spbstu.videomood.btservice.VideoActivityState;
import ru.spbstu.videomood.btservice.MuseState;
import ru.spbstu.videomood.btservice.VideoItem;
import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomood.database.SeanceDataEntry;
import ru.spbstu.videomood.database.SeanceVideo;
import ru.spbstu.videomood.database.User;
import ru.spbstu.videomood.database.Video;
import ru.spbstu.videomood.database.VideoMoodDbHelper;
import ru.spbstu.videomoodadmin.AdminConst;
import ru.spbstu.videomoodadmin.Debug;
import ru.spbstu.videomoodadmin.HorseshoeView;
import ru.spbstu.videomoodadmin.R;
import ru.spbstu.videomoodadmin.UserViewModel;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends OrmLiteBaseActivity<VideoMoodDbHelper> {

    private Timer debugTimerPacketSender;

    private static final String TAG = "MainActivity";

    private VideoMoodDbHelper dbHelper;
    private Dao<User, Integer> userDao;
    private Dao<Seance, Integer> seanceDao;

    private VideoActivityState testVideoActivityState;

    // Intent request codes
    public static final int REQUEST_SELECT_VIDEO = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    private BarChart chart;
    private HorseshoeView sensorsChart;
    private LinearLayout videoControl;

    private int time = 0;

    private static final int CHART_SIZE = 60;
    private MuseState museState = MuseState.DISCONNECTED;

    private BarDataSet createSet(String name, int color) {
        ArrayList<BarEntry> values = new ArrayList<>();
        values.add(new BarEntry(0,0));
        BarDataSet set = new BarDataSet(values, name);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(color);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private static final int ALPHA_DATA_SET_INDEX = 0;
    private static final int BETA_DATA_SET_INDEX = 1;

    private IBarDataSet alphaSet;
    private IBarDataSet betaSet;

    BarData barData;

    private void addEntryToChart(int alphaValue, int betaValue, boolean isPanic) {
        BarData data = chart.getData();
        if (data == null)
            return;

        time++;

        int currentDataSetIndex = isPanic ? BETA_DATA_SET_INDEX : ALPHA_DATA_SET_INDEX;
        data.addEntry(new BarEntry(time, betaValue), currentDataSetIndex);
        data.addEntry(new BarEntry(time, 0), isPanic ? ALPHA_DATA_SET_INDEX : BETA_DATA_SET_INDEX);
        if (alphaSet.getEntryCount() == CHART_SIZE) {
            alphaSet.removeEntry(0);
            betaSet.removeEntry(0);
        }
        data.notifyDataChanged();

        // let the chart know it's data has changed
        chart.notifyDataSetChanged();

        chart.moveViewToX(data.getEntryCount());
        chart.setVisibleXRangeMinimum(CHART_SIZE);
        chart.setVisibleXRangeMaximum(CHART_SIZE);
        chart.setVisibleYRange(100, 100, YAxis.AxisDependency.LEFT);

        // this automatically refreshes the chart (calls invalidate())
        // mChart.moveViewTo(data.getXValCount()-7, 55f,
        // AxisDependency.LEFT);
    }

    private TextView finishSeanceTextView;

    public void setupUI() {
        initChart();

        setupTextViews();

        videoControl = findViewById(R.id.videoControl);
        sensorsChart = findViewById(R.id.deviceInfo);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ControlPacket cp = new ControlPacket(Command.REWIND, seekBar.getProgress());
                sendPacket(cp);
            }
        });

        finishSeanceTextView = findViewById(R.id.main_finishSeanceBtn);
        setEnabledFinishSeanceButton(false);
        finishSeanceTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishSeance();
            }
        });

        TextView museStatus = findViewById(R.id.museState);
        museStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.this.museState == MuseState.DISCONNECTED)
                    showReconnectMuseDialog();
            }
        });
    }

    private void setEnabledFinishSeanceButton(boolean isEnabled) {
        finishSeanceTextView.setEnabled(isEnabled);
        finishSeanceTextView.setTextColor(getResources().getColor(isEnabled ? android.R.color.white : R.color.colorAccentMuted));
    }

    private void initChart() {
        chart = findViewById(R.id.plotView);

        barData = new BarData();

        XAxis xAx = chart.getXAxis(); //no axis
        YAxis yAx = chart.getAxisLeft();
        YAxis yRightAx = chart.getAxisRight();
        xAx.setEnabled(false);
        yAx.setEnabled(false);
        yRightAx.setEnabled(false);

        chart.setDrawGridBackground(false); //no grid
        chart.setDescription(null); //no description

        chart.setData(barData);
        chart.setBorderWidth(0f);

        alphaSet = createSet(getResources().getString(R.string.calm), getResources().getColor(R.color.calmColor));
        barData.addDataSet(alphaSet);

        betaSet = createSet(getResources().getString(R.string.warning), getResources().getColor(R.color.warningColor));
        barData.addDataSet(betaSet);
    }

    private UserViewModel userViewModel;
    private Seance seance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MessageHandler(this);

        try {
            dbHelper = getHelper();
            userDao = dbHelper.getUserDao();
            seanceDao = dbHelper.getDao(Seance.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        setupUI();

        setupUser();

        if (!Debug.ON) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        } else {
            testVideoActivityState = new VideoActivityState();
            testVideoActivityState.setVideoName("Video Name");
            testVideoActivityState.setMuseState(MuseState.CONNECTED);
            testVideoActivityState.setMuseBatteryPercent(14);
            testVideoActivityState.setAlphaPct(20);
            testVideoActivityState.setBetaPct(80);
            testVideoActivityState.setHeadsetBatteryPercent(68);
            testVideoActivityState.setIsVideoPlaying(true);
            testVideoActivityState.setIsPanic(false);
            testVideoActivityState.setCurrentPositionSec(60);
            testVideoActivityState.setDurationSec(100);
            testVideoActivityState.setMuseSensorsState(new Boolean[]{ true, true, true, true, true });
            videoActivityState = testVideoActivityState;
        }
    }

    private void setupUser() {
        Intent prevIntent = this.getIntent();
        int userId = prevIntent.getIntExtra(AdminConst.EXTRA_USER_ID, -1);
        User user = null;
        try {
            user = userDao.queryForId(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        userViewModel = new UserViewModel(user);
        TextView userFirstName = findViewById(R.id.main_user_firstname);
        userFirstName.setText(userViewModel.firstName);
        TextView userLastName = findViewById(R.id.main_user_lastname);
        userLastName.setText(userViewModel.lastName);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!Debug.ON) {
            // If BT is not on, request that it be enabled
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else if (mBtService == null) {
                setupBtService();
                connectDevice(getIntent());
            }
        } else {
            debugTimerPacketSender = new Timer();
            debugTimerPacketSender.schedule(new TimerTask() {
                private double time;
                private long panicTicks = 0;

                @Override
                public void run() {
                    testVideoActivityState.setBetaPct((int) (Math.sin(time++) * 50.0) + 50);
                    if (panicTicks-- == 0) {
                        testVideoActivityState.setIsPanic(!testVideoActivityState.isPanic());
                        panicTicks = Math.round(5 + Math.random() * 25);
                    }
                    Message msg = new Message();
                    msg.what = Constants.MESSAGE_PACKET;
                    mHandler.sendMessage(msg);
                }
            }, 0, 1000);
        }
    }

    private void setupBtService() {
        mBtService = new BluetoothService(mHandler, BluetoothAdapter.getDefaultAdapter());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!Debug.ON) {
//            // Performing this check in onResume() covers the case in which BT was
//            // not enabled during onStart(), so we were paused to enable it...
//            // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
//            if (mBtService != null) {
//                // Only if the state is STATE_NONE, do we know that we haven't started already
//                if (mBtService.getState() == BluetoothService.STATE_NONE) {
//                    // Start the Bluetooth chat services
//                    mBtService.startServer();
//                }
//            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sendMessageHandler.removeCallbacks(sendMessageRunnable);

        if (!Debug.ON) {
            if (mBtService != null) {
                mBtService.stop();
            }
        } else
            debugTimerPacketSender.cancel();
    }

    private boolean finishing = false;

    private void finishSeance() {
        finishing = true;
        try {
            saveSeanceDataChunk();
            userViewModel.setDateFinish(Calendar.getInstance().getTime());
            seance.setDateTo(userViewModel.getDateFinish());
            seanceDao.update(seance);
            goToSeance(seance);
            finish();
        }
        catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void createSeance() {
        Seance seance = new Seance();
        try {
            seance.user = userDao.queryForId(userViewModel.id);
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(MainActivity.this, R.string.seanceCreateError, Toast.LENGTH_LONG).show();
            return;
        }

        userViewModel.setSeanceDateStart(Calendar.getInstance().getTime());
        seance.setDateFrom(userViewModel.getSeanceDateStartStr());

        saveSeance(seance);

        this.seance = seance;
    }

    private void saveSeance(Seance seance) {
        try {
            if (seanceDao.create(seance) != 1)
                throw new SQLException("No seance created");
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(MainActivity.this, R.string.seanceCreateError, Toast.LENGTH_LONG).show();
        }
    }

    private void goToSeance(Seance seance) {
        Intent intent = new Intent(MainActivity.this, SeanceActivity.class);
        intent.putExtra(AdminConst.EXTRA_SEANCE_ID, seance.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mBtService = null;

    private BluetoothDevice deviceToConnect;

    /**
     * Establish connection with other device
     *
     * @param connectionIntent   An {@link Intent} with extra.
     */
    private void connectDevice(Intent connectionIntent) {
        // Get the device MAC address
        String deviceAddress = connectionIntent.getStringExtra(AdminConst.EXTRA_DEVICE_ADDRESS);

        // Get the BluetoothDevice object

        deviceToConnect = mBluetoothAdapter.getRemoteDevice(deviceAddress);

        Log.i(TAG, String.format("Attempt to connectToServer to %s(%s)", deviceToConnect.getName(), deviceAddress));
        // Attempt to connectToServer to the device
        mBtService.connectToServer(deviceToConnect);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private Handler mHandler;

    private SeekBar seekBar;

    public static final String EXTRA_SELECTED_VIDEO = "selected_video";

    public void selectVideo(View view) {
        Intent selectVideoIntent = new Intent(MainActivity.this, SelectVideoActivity.class);
        startActivityForResult(selectVideoIntent, REQUEST_SELECT_VIDEO);
    }

    public void pause(View view) { sendPacket(new ControlPacket(Command.PAUSE)); }

    public void next(View view) {
        sendPacket(new ControlPacket(Command.NEXT));
    }

    public void prev(View view) {
        sendPacket(new ControlPacket(Command.PREV));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_VIDEO) {
            if (resultCode == RESULT_OK) {
                String videoPath = data.getStringExtra(EXTRA_SELECTED_VIDEO);
                sendPacket(new ControlPacket(Command.PLAY, videoPath));
            }
        }
    }

    private int headsetStatus;

    private void setHeadsetStatus(int status) {
        headsetStatus = status;

        int stringResId;
        int colorResId;
        switch (status) {
            case BluetoothService.STATE_CONNECTED:
                stringResId = R.string.state_connected;
                colorResId = R.color.connectedColor;
                break;
            case BluetoothService.STATE_CONNECTING:
                stringResId = R.string.state_connecting;
                colorResId = R.color.connectedColor;
                break;
            case BluetoothService.STATE_NONE:
                stringResId = R.string.state_not_connected;
                colorResId = R.color.disconnectedColor;
                break;
            default:
                stringResId = R.string.state_unknown;
                colorResId = R.color.disconnectedColor;
                break;
        }
        headsetStateTextView.setTextColor(getResources().getColor(colorResId));
        headsetStateTextView.setText(stringResId);

        userIcon.setTextColor(getResources().getColor(R.color.colorPrimary));
        userIcon.setText(R.string.fa_user);
    }

    private Handler sendMessageHandler = new Handler();

    private Runnable sendMessageRunnable = new Runnable() {
        @Override
        public void run() {
            ControlPacket controlPacket = new ControlPacket(Command.GET);
            sendPacket(controlPacket);
            sendMessageHandler.postDelayed(sendMessageRunnable, 1000);
        }
    };

    private void sendPacket(ControlPacket controlPacket) {
        if (mBtService != null) {
            byte[] msgBytes = controlPacket.toBytes();
            mBtService.write(msgBytes);
        }
    }

    private VideoActivityState videoActivityState = new VideoActivityState();

    private static class MessageHandler extends Handler {
        private WeakReference<MainActivity> activity;
        MessageHandler(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            activity.get().onHeadsetConnected();
                            break;
                        case BluetoothService.STATE_NONE:
                            activity.get().onHeadsetDisconnected();
                            break;
                    }
                    activity.get().setHeadsetStatus(msg.arg1);
                    break;
                case Constants.MESSAGE_PACKET:
                    if (!Debug.ON) {
                        VideoActivityState state = null;
                        try {
                            state = VideoActivityState.createFrom((String) msg.obj);
                        } catch (Exception e) {
                            Log.e(TAG, "Error packet creation from " + msg.obj, e);
                        }
                        if (state != null)
                            activity.get().processPacketData(state);
                        else
                            Log.d(TAG, "unable to process data packet cause its null");
                    }
                    break;
            }
        }
    }

    private void onHeadsetConnected() {
        createSeance();
        sendPacket(new ControlPacket(Command.LIST));
        setEnabledFinishSeanceButton(true);
        showProgressDialog();
    }

    private void onHeadsetDisconnected() {
        onMuseDisconnected();
        sendMessageHandler.removeCallbacks(sendMessageRunnable);
        if (pd != null && pd.isShowing())
            hideProgressDialog();
        if (headsetStatus == BluetoothService.STATE_CONNECTING
                || headsetStatus == BluetoothService.STATE_CONNECTED)
            showReconnectHeadsetDialog();
    }

    private ProgressDialog pd = null;

    private void showProgressDialog() {
        pd = new ProgressDialog(MainActivity.this);
        pd.setTitle(getString(R.string.connectingProgressTitle));
        pd.setMessage(getString(R.string.connectionProgressMessage));
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
    }

    private void hideProgressDialog() {
        pd.setIndeterminate(false);
        pd.dismiss();
        pd = null;
    }

    private TextView museStatusTextView;
    private TextView museBatteryTextView;
    private TextView headsetStateTextView;
    private TextView headsetBatteryTextView;
    private TextView videoNameTextView;
    private TextView pauseBtn;
    private TextView userIcon;

    private void setFont(Typeface font, int id) {
        TextView item = findViewById(id);
        setFont(font, item);
    }

    private void setFont(Typeface font, TextView textView) {
        textView.setTypeface(font);
    }

    private void setupTextViews() {
        museStatusTextView = findViewById(R.id.museState);
        museBatteryTextView = findViewById(R.id.museBattery);
        headsetStateTextView = findViewById(R.id.headsetState);
        headsetBatteryTextView = findViewById(R.id.headsetBattery);
        videoNameTextView = findViewById(R.id.videoName);
        pauseBtn = findViewById(R.id.playBtn);
        userIcon = findViewById(R.id.userIcon);

        userIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNotification();
            }
        });

        Typeface font = Typeface.createFromAsset( getAssets(), "fonts/fontawesome.ttf" );

        setFont(font, R.id.prevBtn);
        setFont(font, R.id.nextBtn);
        setFont(font, R.id.userIcon);
        setFont(font, R.id.videoSelect);
        setFont(font, pauseBtn);
    }

    private int calcBatteryTextColor(int percents) {
        if (percents < 20) {
            return getResources().getColor(R.color.batteryLowColor);
        }
        else {
            return getResources().getColor(R.color.batteryHighColor);
        }
    }

    private int getMuseStateString(MuseState museState) {
        switch (museState) {
            case CONNECTED:
                return R.string.state_connected;
            case CONNECTING:
                return R.string.state_connecting;
            case DISCONNECTED:
                return R.string.state_not_connected;
            default:
                return R.string.state_unknown;
        }
    }

    private int getColorOfMuseState(MuseState museState) {
        switch (museState) {
            case CONNECTED:
                return R.color.connectedColor;
            case CONNECTING:
            case DISCONNECTED:
            default:
                return R.color.disconnectedColor;
        }
    }

    private void updateMuseStatus(MuseState museState) {
        int stateStringId = getMuseStateString(museState);
        int stateStringColorId = getColorOfMuseState(museState);
        museStatusTextView.setText(stateStringId);
        museStatusTextView.setTextColor(getResources().getColor(stateStringColorId));
    }

    private boolean prevIsPanic = false;

    private void switchToPanic() {
        userIcon.setText(R.string.fa_frown_o);
        userIcon.setTextColor(getResources().getColor(R.color.warningColor));
    }

    private void switchToCalm() {
        userIcon.setText(R.string.fa_smile_o);
        userIcon.setTextColor(getResources().getColor(R.color.calmColor));
    }

    private void processPacketData(VideoActivityState state) {
        Integer alphaPct = state.getAlphaPct();
        Integer betaPct = state.getBetaPct();
        boolean isPanic = state.isPanic();
        if (alphaPct != null && betaPct != null) {
            addEntryToChart(alphaPct, betaPct, isPanic);
            addEntryToSeanceData(betaPct, isPanic);
        }

        if (!prevIsPanic && isPanic) {
            switchToPanic();
            playNotification();
        } else if (prevIsPanic && !isPanic) {
            switchToCalm();
        }

        this.prevIsPanic = isPanic;

        MuseState newMuseState = state.getMuseState();
        MuseState prevMuseState = videoActivityState.getMuseState();
        if (newMuseState != prevMuseState) {
            onMuseStateChanged(newMuseState);
        }

        this.museState = newMuseState;

        Integer museBatteryPercent = state.getMuseBatteryPercent();
        updateMuseBatteryPercent(museBatteryPercent);

        Boolean[] sensorsState = state.getMuseSensorsState();
        updateMuseSensorsState(sensorsState);

        Integer headsetBatteryPercent = state.getHeadsetBatteryPercent();
        updateHeadsetBatteryPercent(headsetBatteryPercent);

        String videoname = state.getVideoName();
        updateVideoControl(state, videoname);

        ArrayList<VideoItem> videoItems = state.getVideoList();
        if (videoItems != null) {
            syncVideosWithDb(videoItems);
            state.setVideoList(null);
            hideProgressDialog();
            sendMessageHandler.removeCallbacks(sendMessageRunnable);
            sendMessageHandler.postDelayed(sendMessageRunnable, 1000);
        }

        videoActivityState = state;
    }

    private void updateVideoControl(VideoActivityState state, String videoName) {
        if (videoName != null && !videoName.equals("")) {
            updateVideoName(videoName);

            Boolean isVideoPlaying = state.getIsVideoPlaying();
            pauseBtn.setText(isVideoPlaying != null && isVideoPlaying ? R.string.fa_pause : R.string.fa_play);

            updateVideoTimeValues(state.getCurrentPositionSec(),  state.getDurationSec());

            videoControl.setVisibility(View.VISIBLE);
        }
        else {
            videoNameTextView.setText(R.string.videoNotPlayed);
            videoControl.setVisibility(View.INVISIBLE);
        }
    }

    private void updateVideoTimeValues(Integer videoPosition, Integer videoDuration) {
        if (videoPosition == null || videoDuration == null)
            return;

        seekBar.setProgress(videoPosition);
        seekBar.setMax(videoDuration);

        TextView currentPositionTv = findViewById(R.id.main_seekBarCurrentPosition);
        currentPositionTv.setText(String.format("%d:%02d", videoPosition / 60, videoPosition % 60));

        TextView durationTv = findViewById(R.id.main_seekBarDuration);
        durationTv.setText(String.format("%d:%02d", videoDuration / 60, videoDuration % 60));
    }

    private void updateVideoName(String videoName) {
        String currentVideoName = userViewModel.getCurrentVideoName();
        if (!videoName.equals(currentVideoName)) {
            saveSeanceDataChunk();
            userViewModel.setCurrentVideoName(videoName);
        }

        videoNameTextView.setText(videoName);
    }

    private void updateHeadsetBatteryPercent(Integer headsetBatteryPercent) {
        if (headsetBatteryPercent != null)
        {
            headsetBatteryTextView.setText(getString(R.string.defaultPercentFormatString, headsetBatteryPercent));
            headsetBatteryTextView.setVisibility(View.VISIBLE);
        }
        else {
            headsetBatteryTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void onMuseStateChanged(@NotNull MuseState newMuseState) {
        switch (newMuseState) {
            case CONNECTING:
                onMuseConnecting();
                break;
            case CONNECTED:
                onMuseConnected();
                break;
            case DISCONNECTED:
                onMuseDisconnected();
                break;
        }
        updateMuseStatus(newMuseState);
    }

    private void addEntryToSeanceData(Integer betaPct, boolean isPanic) {
        if (userViewModel != null) {
            SeanceDataEntry seanceDataEntry = new SeanceDataEntry();
            seanceDataEntry.betaValue = betaPct;
            seanceDataEntry.isPanic = isPanic;
            userViewModel.seanceData.add(seanceDataEntry);
        }
    }

    private void onMuseConnecting() {
    }

    private void onMuseConnected() {
        failedMuseReconnectCount = 0;
    }

    private void onMuseDisconnected() {
        hideMuseTextViews();

        if (failedMuseReconnectCount < MAX_FAILED_MUSE_RECONNECT) {
            failedMuseReconnectCount++;
            reconnectMuse();
        }
        else {
            failedMuseReconnectCount = 0;
            showReconnectMuseDialog();
        }
    }

    private void updateMuseSensorsState(Boolean[] sensorsState) {
        sensorsChart.setVisibility(sensorsState == null ? View.INVISIBLE : View.VISIBLE);
        if (sensorsState != null)
            sensorsChart.setCircles(sensorsState);
    }

    private void updateMuseBatteryPercent(Integer museBatteryPercent) {
        museBatteryTextView.setVisibility(museBatteryPercent == null ? View.INVISIBLE : View.VISIBLE);
        if (museBatteryPercent != null) {
            museBatteryTextView.setTextColor(calcBatteryTextColor(museBatteryPercent));
            museBatteryTextView.setText(getString(R.string.defaultPercentFormatString, museBatteryPercent));
        }
    }

    private void hideMuseTextViews() {
        museBatteryTextView.setVisibility(View.INVISIBLE);
        sensorsChart.setVisibility(View.INVISIBLE);
    }

    private void showReconnectHeadsetDialog() {
        if (isFinishing() || (pd != null && pd.isShowing()))
            return;

        Resources resources = getResources();
        new AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.reconnect_headset_header))
                .setMessage(resources.getString(R.string.reconnect_headset_message))
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
                        mBtService.connectToServer(deviceToConnect);
                    }
                })
                .show();
    }

    private void showReconnectMuseDialog() {
        Resources resources = getResources();
        new AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.reconnect_muse_header))
                .setMessage(resources.getString(R.string.reconnect_muse_message))
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

    private int failedMuseReconnectCount = 0;
    private static final int MAX_FAILED_MUSE_RECONNECT = 10;

    private void reconnectMuse() {
        sendPacket(new ControlPacket(Command.RECONNECT_MUSE));
    }

    private void playNotification() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveSeanceDataChunk() {
        try {
            Dao<Video, Integer> videoDao = dbHelper.getDao(Video.class);
            List<Video> videoEntities = videoDao.queryForEq("path", userViewModel.getCurrentVideoName());
            if (videoEntities.isEmpty())
                return;

            Video currentVideo = videoEntities.get(0);

            Dao<SeanceVideo, Integer> seanceVideoDao = dbHelper.getDao(SeanceVideo.class);
            SeanceVideo seanceVideo = new SeanceVideo();
            seanceVideo.video = currentVideo;
            seanceVideo.seance = this.seance;

            Calendar seanceStart = Calendar.getInstance();
            seanceStart.setTime(userViewModel.getSeanceDateStart());

            seanceVideo.setData(userViewModel.seanceData);
            userViewModel.seanceData.clear();

            seanceVideoDao.create(seanceVideo);
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void syncVideosWithDb(ArrayList<VideoItem> remoteVideoList) {
        List<Video> dbVideoList;
        Dao<Video, Integer> videoDao;

        try {
            videoDao = dbHelper.getDao(Video.class);
            dbVideoList = videoDao.queryForAll();
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        //fill the set of remote video names
        HashSet<String> remoteVideosNames = new HashSet<>();
        for (VideoItem remoteVideo : remoteVideoList)
            remoteVideosNames.add(remoteVideo.getName());

        HashSet<String> dbVideosNames = new HashSet<>();
        for (Video dbVideo : dbVideoList) {
            String dbVideoName = dbVideo.getName();
            if (remoteVideosNames.contains(dbVideoName))
                //fill the set of db video names
                dbVideosNames.add(dbVideoName);
            else
                //remove absent videos from db
                try {
                    videoDao.delete(dbVideo);
                } catch (SQLException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
        }

        for (VideoItem remoteVideo : remoteVideoList) {
            String remoteVideoName = remoteVideo.getName();
            //skip existing db videos
            if (dbVideosNames.contains(remoteVideoName))
                continue;

            //add absent remote videos to db
            Video video = new Video();
            video.setName(remoteVideo.getName());
            video.setPath(remoteVideo.getName());
            video.setDuration(remoteVideo.getDuration());
            try {
                videoDao.create(video);
            } catch (SQLException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            finish();

        } else {
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, R.string.press_back_again_to_exit, Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }
}
