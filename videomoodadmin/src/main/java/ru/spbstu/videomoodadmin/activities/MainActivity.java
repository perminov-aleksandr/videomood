package ru.spbstu.videomoodadmin.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import ru.spbstu.videomood.database.User;
import ru.spbstu.videomood.database.VideoMoodDbHelper;
import ru.spbstu.videomoodadmin.AdminConst;
import ru.spbstu.videomoodadmin.HorseshoeView;
import ru.spbstu.videomoodadmin.R;
import ru.spbstu.videomoodadmin.UserViewModel;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class MainActivity extends OrmLiteBaseActivity<VideoMoodDbHelper> {

    private final boolean IS_DEBUG = false;

    private DataPacket testDataPacket;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String TAG = "MainActivity";

    private TextView connectionStatus;
    private BarChart chart;
    private HorseshoeView sensorsChart;
    private LinearLayout videoControl;

    private int time = 0;

    private final int chartSize = 60;
    public static ArrayList<String> videoItems = new ArrayList<>();

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

    //todo: extract init chart and update data
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
            chart.setVisibleXRangeMinimum(60);
            chart.setVisibleXRangeMaximum(60);
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

        final Button finishSeance = (Button) findViewById(R.id.main_finishSeanceBtn);
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

    private UserViewModel user;

    private Dao<User, Integer> userDao;
    private Dao<Seance, Integer> seanceDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            userDao = getHelper().getUserDao();
            seanceDao = getHelper().getDao(Seance.class);
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
        this.user = new UserViewModel(user);
        TextView userFirstName = (TextView) findViewById(R.id.main_user_firstname);
        userFirstName.setText(this.user.firstName);
        TextView userLastName = (TextView) findViewById(R.id.main_user_lastname);
        userLastName.setText(this.user.lastName);
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
                user.setSeanceDateStart(Calendar.getInstance().getTime());
            }
        } else {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
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
        mBtService = new BluetoothService(mHandler);
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
                    mBtService.start();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (!IS_DEBUG) {
            if (mBtService != null) {
                mBtService.stop();
            }
        }

        if (user != null && user.seanceData.size() > 0) {
            finishSeance();
        }
    }

    private void finishSeance() {
        Seance seance = new Seance();
        try {
            seance.user = userDao.queryForId(user.id);
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(MainActivity.this, R.string.seanceCreateError, Toast.LENGTH_LONG);
            return;
        }

        seance.setDateFrom(user.getSeanceDateStart());
        String dateTo = user.getDateFinish();
        if (dateTo == null) {
            user.setDateFinish(Calendar.getInstance().getTime());
            dateTo = user.getDateFinish();
        }
        seance.setDateTo(dateTo);
        seance.setData(user.seanceData);

        try {
            if (seanceDao.create(seance) != 1)
                throw new SQLException("No seance created");
            user = null;
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(MainActivity.this, R.string.seanceCreateError, Toast.LENGTH_LONG);
            return;
        }

        goToSeance(seance);
        finishActivity(RESULT_OK);
    }

    private void goToSeance(Seance seance) {
        Intent intent = new Intent(MainActivity.this, SeanceActivity.class);
        intent.putExtra(AdminConst.EXTRA_SEANCE_ID, seance.getId());
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

        Log.i(TAG, String.format("Attempt to connect to %s(%s)", device.getName(), deviceAddress));
        // Attempt to connect to the device
        mBtService.connect(device);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new MessageHandler();

    private static final int SELECT_VIDEO_REQUEST = 1;

    private SeekBar seekBar;

    public static final String EXTRA_SELECTED_VIDEO = "selected_video";

    public void selectVideo(View view) {
        sendPacket(new ControlPacket(Command.LIST));
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
        if (requestCode == SELECT_VIDEO_REQUEST) {
            if (resultCode == RESULT_OK) {
                long videoIndex = data.getLongExtra(EXTRA_SELECTED_VIDEO, -1);
                sendPacket(new ControlPacket(Command.PLAY, videoIndex));
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
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(R.string.state_connected);
                            sendMessageHandler.postDelayed(sendMessageRunnable, 1000);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.state_connecting);
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
                        dataPacket = DataPacket.createFrom((String)msg.obj);
                    }
                    if (dataPacket != null)
                        processPacketData();
                    break;
            }
        }
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

    private void processPacketData() {
        Integer alphaPct = dataPacket.getAlphaPct();
        Integer betaPct = dataPacket.getBetaPct();
        boolean isPanic = dataPacket.isPanic();
        if (alphaPct != null && betaPct != null) {
            addEntry(alphaPct, betaPct, isPanic);

            if (user != null) {
                SeanceDataEntry seanceDataEntry = new SeanceDataEntry();
                seanceDataEntry.betaValue = betaPct;
                seanceDataEntry.isPanic = isPanic;
                user.seanceData.add(seanceDataEntry);
            }
        }
        userIcon.setText(isPanic ? R.string.fa_frown_o : R.string.fa_smile_o);
        userIcon.setTextColor(getResources().getColor(isPanic ? R.color.warningColor : R.color.calmColor));

        Boolean isMuseConnected = dataPacket.getMuseState();
        if (isMuseConnected != null && isMuseConnected) {
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
            } else {
                sensorsChart.setVisibility(View.INVISIBLE);
            }
        }
        else {
            museStatusTextView.setText(R.string.state_not_connected);
            museStatusTextView.setTextColor(getResources().getColor(R.color.disconnectedColor));
            museBatteryTextView.setVisibility(View.INVISIBLE);
        }

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
            videoNameTextView.setText(videoname);
            Boolean videoState = dataPacket.getVideoState();
            pauseBtn.setText(videoState != null && videoState ? R.string.fa_pause : R.string.fa_play);

            Integer videoPosition = dataPacket.getCurrentPositionSec();
            Integer videoDuration = dataPacket.getDurationSec();
            if (videoPosition != null && videoDuration != null) {
                seekBar.setProgress(videoPosition);
                seekBar.setMax(videoDuration);

                TextView currentPostionTv = (TextView)findViewById(R.id.main_seekBarCurrentPosition);
                currentPostionTv.setText(String.format("%d:%02d", videoPosition / 60, videoPosition % 60));

                TextView durationTv = (TextView)findViewById(R.id.main_seekBarDuration);
                durationTv.setText(String.format("%d:%02d", videoDuration / 60, videoDuration % 60));
            }

            videoControl.setVisibility(View.VISIBLE);
        }
        else {
            videoNameTextView.setText(R.string.videoNotPlayed);
            videoControl.setVisibility(View.INVISIBLE);
        }

        ArrayList<VideoItem> lVideoItems = dataPacket.getVideoList();
        if (lVideoItems != null) {
            videoItems.clear();
            for (VideoItem videoItem : lVideoItems)
                videoItems.add(videoItem.getName());

            Intent intent = new Intent(this, SelectVideoActivity.class);
            intent.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivityForResult(intent, SELECT_VIDEO_REQUEST);
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
