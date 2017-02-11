package ru.spbstu.videomoodadmin.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;

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

    private TextView connectionStatus;
    private BarChart chart;
    private LinearLayout videoControl;
    private View mainView;

    private int time = 0;

    private final int chartSize = 60;

    private BarDataSet createSet() {
        ArrayList<BarEntry> vals = new ArrayList<>();
        vals.add(new BarEntry(0,0));
        BarDataSet set = new BarDataSet(vals, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }


    private int defaultDataSetIndex = 0;

    private void addEntry(int alphaValue) {

        BarData data = chart.getData();

        if (data != null) {

            IBarDataSet set = data.getDataSetByIndex(defaultDataSetIndex);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            time++;

            data.addEntry(new BarEntry(time, (float) Math.sin(time)), defaultDataSetIndex);
            if (set.getEntryCount() == chartSize)
                set.removeEntry(0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(60);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    public void setupUI() {
        connectionStatus = (TextView) findViewById(R.id.connectionStatusLabel);
        chart = (BarChart) findViewById(R.id.plotView);

        BarData data = new BarData();
        chart.setData(data);

        setupTextViews();

        videoControl = (LinearLayout) findViewById(R.id.videoControl);
        mainView = findViewById(R.id.main);
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
        connectionStatus.setText(stringResId);
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
        Log.i("VideoMood Admin", "send command " + controlPacket.getCommand());
        String serializedPacket = new Gson().toJson(controlPacket);
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
                            mainView.setVisibility(View.VISIBLE);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.state_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.state_not_connected);
                            mainView.setVisibility(View.INVISIBLE);
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
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
    private TextView pauseTextView;

    private void setupTextViews() {
        museStatusTextView = (TextView) findViewById(R.id.museState);
        museBatteryTextView = (TextView) findViewById(R.id.museBattery);
        headsetBatteryTextView = (TextView) findViewById(R.id.headsetBattery);
        videoNameTextView = (TextView) findViewById(R.id.videoName);
        pauseTextView = (TextView) findViewById(R.id.playBtn);
    }

    private void processPacketData() {
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

        headsetBatteryTextView.setText(getString(R.string.defaultPercentFormatString, dataPacket.getHeadsetBatteryPercent()));

        String videoname = dataPacket.getVideoName();
        if (videoname != null && !videoname.equals("")) {
            videoNameTextView.setText(videoname);
            pauseTextView.setText(dataPacket.getVideoState() ? "Pause" : "Play");
            videoControl.setVisibility(View.VISIBLE);
        }
        else {
            videoNameTextView.setText(R.string.videoNotPlayed);
            videoControl.setVisibility(View.INVISIBLE);
        }

        ArrayList<VideoItem> lVideoItems = dataPacket.getVideoList();
        /*if (lVideoItems != null) {
            videoItems.clear();
            for (VideoItem videoItem : lVideoItems)
                videoItems.add(videoItem.getName());

            Intent intent = new Intent(this, SelectVideoActivity.class);
            intent.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivityForResult(intent, SELECT_VIDEO_REQUEST);
        }*/
    }
}
