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
import android.support.annotation.Nullable;
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
import ru.spbstu.videomood.btservice.DataPacket;
import ru.spbstu.videomood.btservice.VideoItem;
import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomood.database.SeanceDataEntry;
import ru.spbstu.videomood.database.SeanceVideo;
import ru.spbstu.videomood.database.User;
import ru.spbstu.videomood.database.Video;
import ru.spbstu.videomood.database.VideoMoodDbHelper;
import ru.spbstu.videomoodadmin.AdminConst;
import ru.spbstu.videomoodadmin.HorseshoeView;
import ru.spbstu.videomoodadmin.R;
import ru.spbstu.videomoodadmin.UserViewModel;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends OrmLiteBaseActivity<VideoMoodDbHelper> {

    private final boolean IS_DEBUG = false;

    private Timer debugTimerPacketSender;

    private static final String TAG = "MainActivity";

    private VideoMoodDbHelper dbHelper;
    private Dao<User, Integer> userDao;
    private Dao<Seance, Integer> seanceDao;

    private DataPacket testDataPacket;

    // Intent request codes
    public static final int REQUEST_SELECT_VIDEO = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    private BarChart chart;
    private HorseshoeView sensorsChart;
    private LinearLayout videoControl;

    private int time = 0;

    private final int chartSize = 60;
    private Boolean isMuseConnected = false;

    private BarDataSet createSet(String name, int color) {
        ArrayList<BarEntry> vals = new ArrayList<>();
        vals.add(new BarEntry(0,0));
        BarDataSet set = new BarDataSet(vals, name);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(color);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private int alphaDataSetIndex = 0;
    private int betaDataSetIndex = 1;

    private IBarDataSet alphaSet;
    private IBarDataSet betaSet;

    BarData barData;

    private void addEntry(int alphaValue, int betaValue, boolean isPanic) {
        BarData data = chart.getData();

        if (data != null) {
            time++;

            int currentDataSetIndex = isPanic ? betaDataSetIndex : alphaDataSetIndex;
            data.addEntry(new BarEntry(time, betaValue), currentDataSetIndex);
            data.addEntry(new BarEntry(time, 0), isPanic ? alphaDataSetIndex : betaDataSetIndex);
            if (alphaSet.getEntryCount() == chartSize) {
                alphaSet.removeEntry(0);
                betaSet.removeEntry(0);
            }
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            chart.moveViewToX(data.getEntryCount());
            chart.setVisibleXRangeMinimum(chartSize);
            chart.setVisibleXRangeMaximum(chartSize);
            chart.setVisibleYRange(100, 100, YAxis.AxisDependency.LEFT);

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    public void setupUI() {
        initChart();

        setupTextViews();

        videoControl = (LinearLayout) findViewById(R.id.videoControl);
        sensorsChart = (HorseshoeView) findViewById(R.id.deviceInfo);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
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

        final TextView finishSeance = (TextView) findViewById(R.id.main_finishSeanceBtn);
        finishSeance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishSeance();
            }
        });
    }

    private void initChart() {
        chart = (BarChart) findViewById(R.id.plotView);

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

        try {
            dbHelper = getHelper();
            userDao = dbHelper.getUserDao();
            seanceDao = dbHelper.getDao(Seance.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        setupUI();

        setupUser();

        if (!IS_DEBUG) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        } else {
            testDataPacket = new DataPacket();
            testDataPacket.setVideoName("Video Name");
            testDataPacket.setMuseState(true);
            testDataPacket.setMuseBatteryPercent(14);
            testDataPacket.setAlphaPct(20);
            testDataPacket.setBetaPct(80);
            testDataPacket.setHeadsetBatteryPercent(68);
            testDataPacket.setVideoState(true);
            testDataPacket.setIsPanic(false);
            testDataPacket.setCurrentPositionSec(60);
            testDataPacket.setDurationSec(100);
            testDataPacket.setMuseSensorsState(new Boolean[]{ true, true, true, true, true });
            dataPacket = testDataPacket;
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
        TextView userFirstName = (TextView) findViewById(R.id.main_user_firstname);
        userFirstName.setText(userViewModel.firstName);
        TextView userLastName = (TextView) findViewById(R.id.main_user_lastname);
        userLastName.setText(userViewModel.lastName);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!IS_DEBUG) {
            // If BT is not on, request that it be enabled
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else if (mBtService == null) {
                setupBtService();
                connectDevice(getIntent());
                seance = createSeance();
                saveSeance();
            }
        } else {
            debugTimerPacketSender = new Timer();
            debugTimerPacketSender.schedule(new TimerTask() {
                private double time;
                private long panicTicks = 0;

                @Override
                public void run() {
                    testDataPacket.setBetaPct((int) (Math.sin(time++) * 50.0) + 50);
                    if (panicTicks-- == 0) {
                        testDataPacket.setIsPanic(!testDataPacket.isPanic());
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

        if (!IS_DEBUG) {
            // Performing this check in onResume() covers the case in which BT was
            // not enabled during onStart(), so we were paused to enable it...
            // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
            if (mBtService != null) {
                // Only if the state is STATE_NONE, do we know that we haven't started already
                if (mBtService.getState() == BluetoothService.STATE_NONE) {
                    // Start the Bluetooth chat services
                    mBtService.startServer();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sendMessageHandler.removeCallbacks(sendMessageRunnable);

        if (!IS_DEBUG) {
            if (mBtService != null) {
                mBtService.stop();
            }
        } else
            debugTimerPacketSender.cancel();
    }

    private void finishSeance() {
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

    @Nullable
    private Seance createSeance() {
        Seance seance = new Seance();
        try {
            seance.user = userDao.queryForId(userViewModel.id);
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(MainActivity.this, R.string.seanceCreateError, Toast.LENGTH_LONG).show();
            return null;
        }

        userViewModel.setSeanceDateStart(Calendar.getInstance().getTime());
        seance.setDateFrom(userViewModel.getSeanceDateStartStr());
        return seance;
    }

    private void saveSeance() {
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

    /**
     * Establish connection with other device
     *
     * @param connectionIntent   An {@link Intent} with extra.
     */
    private void connectDevice(Intent connectionIntent) {
        // Get the device MAC address
        String deviceAddress = connectionIntent.getStringExtra(AdminConst.EXTRA_DEVICE_ADDRESS);

        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);

        Log.i(TAG, String.format("Attempt to connectToServer to %s(%s)", device.getName(), deviceAddress));
        // Attempt to connectToServer to the device
        mBtService.connectToServer(device);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new MessageHandler();

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

    private void setStatus(int stringResId) {
       // connectionStatus.setText(stringResId);
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

    private DataPacket dataPacket;

    private class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.state_connecting);
                            break;
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(R.string.state_connected);
                            sendPacket(new ControlPacket(Command.LIST));
                            showProgressDialog();
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.state_not_connected);
                            sendMessageHandler.removeCallbacks(sendMessageRunnable);
                            break;
                    }
                    break;
                case Constants.MESSAGE_PACKET:
                    if (!IS_DEBUG) {
                        try {
                            dataPacket = DataPacket.createFrom((String)msg.obj);
                        } catch (Exception e) {
                            Log.e(TAG, "Error packet creation from " + msg.obj, e);
                        }
                    }
                    if (dataPacket != null)
                        processPacketData();
                    break;
            }
        }
    }

    private ProgressDialog pd;

    private void showProgressDialog() {
        pd = new ProgressDialog(MainActivity.this);
        pd.setTitle(getString(R.string.connectingProgressTitle));
        pd.setMessage(getString(R.string.connectionProgressMessage));
        // меняем стиль на индикатор
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // включаем анимацию ожидания
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
    }

    private TextView museStatusTextView;
    private TextView museBatteryTextView;
    private TextView headsetStateTextView;
    private TextView headsetBatteryTextView;
    private TextView videoNameTextView;
    private TextView pauseBtn;
    private TextView userIcon;

    private void setFont(Typeface font, int id) {
        TextView item = (TextView) findViewById(id);
        setFont(font, item);
    }

    private void setFont(Typeface font, TextView textView) {
        textView.setTypeface(font);
    }

    private void setupTextViews() {
        museStatusTextView = (TextView) findViewById(R.id.museState);
        museBatteryTextView = (TextView) findViewById(R.id.museBattery);
        headsetStateTextView = (TextView) findViewById(R.id.headsetState);
        headsetBatteryTextView = (TextView) findViewById(R.id.headsetBattery);
        videoNameTextView = (TextView) findViewById(R.id.videoName);
        pauseBtn = (TextView) findViewById(R.id.playBtn);
        userIcon = (TextView) findViewById(R.id.userIcon);

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

    private boolean prevIsPanic = false;

    private void processPacketData() {
        Integer alphaPct = dataPacket.getAlphaPct();
        Integer betaPct = dataPacket.getBetaPct();
        boolean isPanic = dataPacket.isPanic();
        if (alphaPct != null && betaPct != null) {
            addEntry(alphaPct, betaPct, isPanic);

            if (userViewModel != null) {
                SeanceDataEntry seanceDataEntry = new SeanceDataEntry();
                seanceDataEntry.betaValue = betaPct;
                seanceDataEntry.isPanic = isPanic;
                userViewModel.seanceData.add(seanceDataEntry);
            }
        }
        userIcon.setText(isPanic ? R.string.fa_frown_o : R.string.fa_smile_o);
        userIcon.setTextColor(getResources().getColor(isPanic ? R.color.warningColor : R.color.calmColor));

        if (!prevIsPanic && isPanic)
            playNotification();

        prevIsPanic = isPanic;

        Boolean _isMuseConnected = dataPacket.getMuseState();
        if (_isMuseConnected) {
            museStatusTextView.setText(R.string.state_connected);
            museStatusTextView.setTextColor(getResources().getColor(R.color.connectedColor));

            Integer museBatteryPercent = dataPacket.getMuseBatteryPercent();
            if (museBatteryPercent != null) {
                museBatteryTextView.setTextColor(calcBatteryTextColor(museBatteryPercent));
                museBatteryTextView.setText(getString(R.string.defaultPercentFormatString, museBatteryPercent));
                museBatteryTextView.setVisibility(View.VISIBLE);
            }

            Boolean[] sensorsState = dataPacket.getMuseSensorsState();
            if (sensorsState != null) {
                sensorsChart.setCircles(sensorsState);
                sensorsChart.setVisibility(View.VISIBLE);
            }
        }
        else {
            museStatusTextView.setText(R.string.state_not_connected);
            museStatusTextView.setTextColor(getResources().getColor(R.color.disconnectedColor));
            museBatteryTextView.setVisibility(View.INVISIBLE);
            sensorsChart.setVisibility(View.INVISIBLE);

            if (isMuseConnected)
                showReconnectDialog();
        }

        isMuseConnected = _isMuseConnected;

        Integer headsetBatteryPercent = dataPacket.getHeadsetBatteryPercent();
        if (headsetBatteryPercent != null)
        {
            headsetStateTextView.setText(R.string.state_connected);
            headsetStateTextView.setTextColor(getResources().getColor(R.color.connectedColor));
            museBatteryTextView.setTextColor(calcBatteryTextColor(headsetBatteryPercent));
            headsetBatteryTextView.setText(getString(R.string.defaultPercentFormatString, headsetBatteryPercent));
            headsetBatteryTextView.setVisibility(View.VISIBLE);
        }
        else {
            headsetStateTextView.setText(R.string.state_not_connected);
            headsetStateTextView.setTextColor(getResources().getColor(R.color.disconnectedColor));
            headsetBatteryTextView.setVisibility(View.INVISIBLE);
        }

        String videoname = dataPacket.getVideoName();
        if (videoname != null && !videoname.equals("")) {
            String currentVideoName = userViewModel.getCurrentVideoName();
            if (!videoname.equals(currentVideoName)) {
                saveSeanceDataChunk();
                userViewModel.setCurrentVideoName(videoname);
            }

            videoNameTextView.setText(videoname);
            Boolean videoState = dataPacket.getVideoState();
            pauseBtn.setText(videoState != null && videoState ? R.string.fa_pause : R.string.fa_play);

            Integer videoPosition = dataPacket.getCurrentPositionSec();
            Integer videoDuration = dataPacket.getDurationSec();
            if (videoPosition != null && videoDuration != null) {
                seekBar.setProgress(videoPosition);
                seekBar.setMax(videoDuration);

                TextView currentPositionTv = (TextView)findViewById(R.id.main_seekBarCurrentPosition);
                currentPositionTv.setText(String.format("%d:%02d", videoPosition / 60, videoPosition % 60));

                TextView durationTv = (TextView)findViewById(R.id.main_seekBarDuration);
                durationTv.setText(String.format("%d:%02d", videoDuration / 60, videoDuration % 60));
            }

            videoControl.setVisibility(View.VISIBLE);
        }
        else {
            videoNameTextView.setText(R.string.videoNotPlayed);
            videoControl.setVisibility(View.INVISIBLE);
        }

        ArrayList<VideoItem> videoItems = dataPacket.getVideoList();
        if (videoItems != null) {
            syncVideosWithDb(videoItems);
            dataPacket.setVideoList(null);
            pd.setIndeterminate(false);
            pd.dismiss();
            sendMessageHandler.removeCallbacks(sendMessageRunnable);
            sendMessageHandler.postDelayed(sendMessageRunnable, 1000);
        }
    }

    private void showReconnectDialog() {
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
                        sendPacket(new ControlPacket(Command.RECONNECT_MUSE));
                    }
                })
                .show();
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
