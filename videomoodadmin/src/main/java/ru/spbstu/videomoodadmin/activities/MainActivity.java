package ru.spbstu.videomoodadmin.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
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
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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

import org.jetbrains.annotations.NotNull;

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
import ru.spbstu.videomood.btservice.MuseState;
import ru.spbstu.videomood.btservice.VideoItem;
import ru.spbstu.videomood.btservice.VideosPacket;
import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomood.database.SeanceDataEntry;
import ru.spbstu.videomood.database.User;
import ru.spbstu.videomood.database.VideoMoodDbHelper;
import ru.spbstu.videomoodadmin.AdminConst;
import ru.spbstu.videomoodadmin.HeadsetManager;
import ru.spbstu.videomoodadmin.HorseshoeView;
import ru.spbstu.videomoodadmin.R;
import ru.spbstu.videomoodadmin.UserViewModel;
import ru.spbstu.videomoodadmin.UsersRepository;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {

    private Timer debugTimerPacketSender;

    private static final String TAG = "MainActivity";

    private DataPacket testDataPacket;

    // Intent request codes
    public static final int REQUEST_SELECT_VIDEO = 1;

    private BarChart chart;
    private HorseshoeView sensorsChart;
    private LinearLayout videoControl;

    private int time = 0;

    private static final int CHART_SIZE = 60;
    private MuseState museState = MuseState.DISCONNECTED;
    private UsersRepository usersRepository;
    private HeadsetManager headsetManager;
    private View museInfo;

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
                headsetManager.sendPacket(cp);
            }
        });

        finishSeanceTextView = findViewById(R.id.main_finishSeanceBtn);
        setEnabledFinishSeanceButton(false);
        finishSeanceTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishSeance();
                goToSeance(seance);
                finish();
            }
        });

        museInfo = findViewById(R.id.museInfo);
        museInfo.setVisibility(View.INVISIBLE);

        TextView museStatus = findViewById(R.id.museState);
        museStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.this.museState == MuseState.DISCONNECTED)
                    showReconnectMuseDialog();
            }
        });

        setupTextViews();
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

        usersRepository = new UsersRepository();
        usersRepository.init(new VideoMoodDbHelper(this));

        setupUI();

        setupUser();
    }

    @Override
    protected void onStart() {
        super.onStart();

        initHeadsetManager();
    }

    private void setupUser() {
        Intent prevIntent = this.getIntent();
        int userId = prevIntent.getIntExtra(AdminConst.EXTRA_USER_ID, -1);

        User user = null;
        try {
            user = usersRepository.get(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (user == null)
            return;

        userViewModel = new UserViewModel(user);
        TextView userFirstName = findViewById(R.id.main_user_firstname);
        userFirstName.setText(userViewModel.firstName);
        TextView userLastName = findViewById(R.id.main_user_lastname);
        userLastName.setText(userViewModel.lastName);
    }

    private boolean finishing = false;

    private void finishSeance() {
        finishing = true;
        try {
            usersRepository.saveSeanceDataChunk(userViewModel, seance);
            userViewModel.setDateFinish(Calendar.getInstance().getTime());
            seance.setDateTo(userViewModel.getDateFinish());
            usersRepository.updateSeance(seance);
        }
        catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void createSeance() {
        Seance seance = new Seance();
        try {
            seance.user = usersRepository.get(userViewModel.id);
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
            if (usersRepository.saveSeance(seance) != 1)
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

    private SeekBar seekBar;

    public static final String EXTRA_SELECTED_VIDEO = "selected_video";

    public void selectVideo(View view) {
        Intent selectVideoIntent = new Intent(MainActivity.this, SelectVideoActivity.class);
        startActivityForResult(selectVideoIntent, REQUEST_SELECT_VIDEO);
    }

    public void pause(View view) { headsetManager.sendPacket(new ControlPacket(Command.PAUSE)); }

    public void next(View view) {
        headsetManager.sendPacket(new ControlPacket(Command.NEXT));
    }

    public void prev(View view) {
        headsetManager.sendPacket(new ControlPacket(Command.PREV));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_VIDEO) {
            if (resultCode == RESULT_OK) {
                String videoPath = data.getStringExtra(EXTRA_SELECTED_VIDEO);
                headsetManager.sendPacket(new ControlPacket(Command.PLAY, videoPath));
            }
        }
    }

    private Integer headsetState = BluetoothService.STATE_NONE;

    private void initHeadsetManager() {
        String deviceAddress = getIntent().getStringExtra(AdminConst.EXTRA_DEVICE_ADDRESS);
        headsetManager = new HeadsetManager(deviceAddress);
        getLifecycle().addObserver(headsetManager);
        headsetManager.getVideoState().observe(this, new Observer<DataPacket>() {
            @Override
            public void onChanged(@Nullable DataPacket dataPacket) {
                processPacketData(dataPacket);
            }
        });
        headsetManager.getHeadsetState().observe(this, new Observer<Integer>(){
            @Override
            public void onChanged(@Nullable Integer headsetState) {
                onHeadsetStateChanged(headsetState);
            }
        });
        headsetManager.getVideos().observe(this, new Observer<VideosPacket>() {
            @Override
            public void onChanged(@Nullable VideosPacket videosPacket) {
                updateVideosList(videosPacket);
            }
        });
    }

    private void onHeadsetStateChanged(Integer headsetState) {
        this.headsetState = headsetState;
        updateHeadsetStateTextView(headsetState);
        switch (headsetState) {
            case BluetoothService.STATE_CONNECTED:
                onHeadsetConnected();
                break;
            case BluetoothService.STATE_NONE:
                onHeadsetDisconnected();
                break;
        }
    }

    private void updateHeadsetStateTextView(int status) {
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

    private DataPacket dataPacket = new DataPacket();

    private void onHeadsetConnected() {
        reconnectHeadsetAttempts = 0;
        createSeance();
        setEnabledFinishSeanceButton(true);
        museInfo.setVisibility(View.VISIBLE);
        showProgressDialog();
    }

    private static final boolean IS_AUTO_RECONNECT_HEADSET = true;
    private static final int MAX_AUTO_RECONNECT_HEADSET_ATTEMPTS = 5;
    private int reconnectHeadsetAttempts = 0;
    private static final int HEADSET_RECONNECT_DELAY = 3*1000;

    private void onHeadsetDisconnected() {
        museInfo.setVisibility(View.INVISIBLE);
        setEnabledFinishSeanceButton(false);

        try {
            usersRepository.saveSeanceDataChunk(userViewModel, seance);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (pd != null && pd.isShowing())
            hideProgressDialog();

        if (IS_AUTO_RECONNECT_HEADSET && reconnectHeadsetAttempts < MAX_AUTO_RECONNECT_HEADSET_ATTEMPTS) {
            reconnectHeadsetAttempts++;
            Timer reconnectTimer = new Timer();
            reconnectTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    headsetManager.reconnect();
                }
            }, HEADSET_RECONNECT_DELAY);
        }
        else
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
        if (pd != null) {
            pd.setIndeterminate(false);
            pd.dismiss();
            pd = null;
        }
    }

    private TextView museStatusTextView;
    private TextView museBatteryTextView;
    private TextView headsetStateTextView;
    private TextView headsetBatteryTextView;
    private TextView videoNameTextView;
    private TextView pauseBtn;
    private TextView userIcon;

    private void setFont(Typeface font, @IdRes int id) {
        TextView item = findViewById(id);
        setFont(font, item);
    }

    private void setFont(Typeface font, @NotNull TextView textView) {
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
        setFontForIcons(font);

        hideMuseTextViews();
        hideHeadsetTextViews();
    }

    private void setFontForIcons(Typeface font) {
        int[] iconViewIds = new int[] { R.id.prevBtn, R.id.nextBtn, R.id.userIcon, R.id.videoSelect, R.id.playBtn };
        for (int viewId: iconViewIds)
            setFont(font, viewId);
    }

    private void hideHeadsetTextViews() {
        headsetBatteryTextView.setVisibility(View.INVISIBLE);
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

    private void processPacketData(DataPacket state) {
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
        MuseState prevMuseState = dataPacket.getMuseState();
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

        updateVideoControl(state);

        dataPacket = state;
    }

    private void updateVideosList(VideosPacket state) {
        ArrayList<VideoItem> videoItems = state.getVideoList();
        if (videoItems != null) {
            try {
                usersRepository.syncVideosWithDb(videoItems);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            state.setVideoList(null);
            hideProgressDialog();
        }
    }

    private void updateVideoControl(DataPacket state) {
        String videoName = state.getVideoName();
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
            try {
                usersRepository.saveSeanceDataChunk(userViewModel, seance);
            } catch (SQLException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }
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
        if (headsetState == BluetoothService.STATE_NONE)
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
                        headsetManager.reconnect();
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
    private static final int MAX_FAILED_MUSE_RECONNECT = 5;

    private void reconnectMuse() {
        headsetManager.sendPacket(new ControlPacket(Command.RECONNECT_MUSE));
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
