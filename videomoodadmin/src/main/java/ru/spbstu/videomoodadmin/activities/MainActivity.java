package ru.spbstu.videomoodadmin.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.joanzapata.iconify.widget.IconTextView;

import java.util.ArrayList;

import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Command;
import ru.spbstu.videomood.btservice.Constants;
import ru.spbstu.videomood.btservice.ControlPacket;
import ru.spbstu.videomood.btservice.DataPacket;
import ru.spbstu.videomood.btservice.VideoItem;
import ru.spbstu.videomoodadmin.R;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class MainActivity extends AppCompatActivity {

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String TAG = "MainActivity";

    private TextView connectionStatus;
    private BarChart chart;
    private LinearLayout videoControl;
    private View mainView;

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
    private void addEntry(int alphaValue, int betaValue) {
        BarData data = chart.getData();

        if (data != null) {
            time++;

            data.addEntry(new BarEntry(time, 100), alphaDataSetIndex);
            data.addEntry(new BarEntry(time, betaValue), betaDataSetIndex);
            if (alphaSet.getEntryCount() == chartSize) {
                alphaSet.removeEntry(0);
                betaSet.removeEntry(0);
            }
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            chart.moveViewToX(data.getEntryCount());
            chart.setVisibleXRangeMaximum(60);

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    public void setupUI() {
        //connectionStatus = (TextView) findViewById(R.id.connectionStatusLabel);
        initChart();

        setupTextViews();

        videoControl = (LinearLayout) findViewById(R.id.videoControl);
       // mainView = findViewById(R.id.main);
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

        alphaSet = createSet("Alpha", Color.rgb(18, 103, 231));
        barData.addDataSet(alphaSet);

        betaSet = createSet("Beta", Color.rgb(215, 69, 46));
        barData.addDataSet(betaSet);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mBtService == null) {
            setupBtService();
            connectDevice(getIntent(), true);
        }
    }

    private void setupBtService() {
        mBtService = new BluetoothService(this, mHandler);
    }

    @Override
    public void onResume() {
        super.onResume();

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
    public void onDestroy() {
        super.onDestroy();
        if (mBtService != null) {
            mBtService.stop();
        }
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
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent connectionIntent, boolean secure) {
        // Get the device MAC address
        String deviceAddress = connectionIntent.getStringExtra(Constants.EXTRA_DEVICE_ADDRESS);

        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        // Attempt to connect to the device
        mBtService.connect(device, secure);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new MessageHandler();

    static final int SELECT_VIDEO_REQUEST = 1;

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
        String serializedPacket = new Gson().toJson(controlPacket);
        Log.i(TAG, "SENDING: " + serializedPacket);
        mBtService.write(serializedPacket.getBytes());
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
                            //mainView.setVisibility(View.VISIBLE);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.state_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.state_not_connected);
                            sendMessageHandler.removeCallbacks(sendMessageRunnable);
                            //mainView.setVisibility(View.INVISIBLE);
                            break;
                    }
                    break;
                case Constants.MESSAGE_PACKET:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.i(TAG, "RECEIVING: " + readMessage);
                    dataPacket = new Gson().fromJson(readMessage, DataPacket.class);
                    processPacketData();
                    break;
            }
        }
    }

    private TextView museStatusTextView;
    private TextView museBatteryTextView;
    private TextView headsetBatteryTextView;
    private TextView videoNameTextView;
    private TextView pauseBtn;

    private void setFont(Typeface font, int id) {
        TextView item = (TextView) findViewById(id);
        item.setTypeface(font);
    }

    private void setupTextViews() {
        museStatusTextView = (TextView) findViewById(R.id.museState);
        museBatteryTextView = (TextView) findViewById(R.id.museBattery);
        headsetBatteryTextView = (TextView) findViewById(R.id.headsetBattery);
        videoNameTextView = (TextView) findViewById(R.id.videoName);
        pauseBtn = (TextView) findViewById(R.id.playBtn);

        Typeface font = Typeface.createFromAsset( getAssets(), "fonts/fontawesome.ttf" );
        pauseBtn.setTypeface(font);

        setFont(font, R.id.prevBtn);
        setFont(font, R.id.nextBtn);
        setFont(font, R.id.userIcon);
        setFont(font, R.id.videoSelect);
    }

    private void processPacketData() {
        Integer alphaPct = dataPacket.getAlphaPct();
        Integer betaPct = dataPacket.getBetaPct();
        if (alphaPct != null && betaPct != null)
            addEntry(alphaPct, betaPct);

        Boolean isMuseConnected = dataPacket.getMuseState();
        if (isMuseConnected != null && isMuseConnected) {
            museStatusTextView.setText(R.string.state_connected);
            museBatteryTextView.setVisibility(View.VISIBLE);
            museBatteryTextView.setText(getString(R.string.defaultPercentFormatString, dataPacket.getMuseBatteryPercent()));
        }
        else {
            museStatusTextView.setText(R.string.state_not_connected);
            museBatteryTextView.setVisibility(View.INVISIBLE);
        }

        Integer headsetBatteryPercent = dataPacket.getHeadsetBatteryPercent();
        if (headsetBatteryPercent != null)
        {
            headsetBatteryTextView.setText(getString(R.string.defaultPercentFormatString, headsetBatteryPercent));
            headsetBatteryTextView.setVisibility(View.VISIBLE);
        }
        else
            headsetBatteryTextView.setVisibility(View.INVISIBLE);

        String videoname = dataPacket.getVideoName();
        if (videoname != null && !videoname.equals("")) {
            videoNameTextView.setText(videoname);
            Boolean videoState = dataPacket.getVideoState();
            pauseBtn.setText(videoState != null && videoState ? R.string.fa_pause : R.string.fa_play);
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
}
