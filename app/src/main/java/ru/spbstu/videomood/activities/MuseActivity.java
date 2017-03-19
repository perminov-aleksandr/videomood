package ru.spbstu.videomood.activities;

import android.app.Activity;
import android.os.Bundle;

import com.choosemuse.libmuse.Battery;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseDataPacket;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ru.spbstu.videomood.ConnectionListener;
import ru.spbstu.videomood.DataListener;
import ru.spbstu.videomood.MuseManager;

public abstract class MuseActivity extends BaseActivity {
    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

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

    private void setupMuseManager() {
        MuseManager.setContext(this);
        MuseManager.registerMuseListeners(connectionListener, dataListener);

        // Cache the Muse that the user has selected.
        muse = MuseManager.getMuse();
        //start receiving muse packets
        muse.runAsynchronously();
    }

    protected void reconnectMuse() {
        muse.runAsynchronously();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WeakReference<MuseActivity> weakActivity = new WeakReference<>(this);
        // Register a listener to handle connection state changes
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);

        setupMuseManager();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (muse != null)
            muse.enableDataTransmission(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (muse != null)
            muse.enableDataTransmission(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (muse != null) {
            muse.unregisterAllListeners();
            muse.disconnect(false);
        }
    }

    private ConnectionState connectionState;
    protected ConnectionState getConnectionState() {
        return connectionState;
    }

    protected boolean isConnectionStatusStale = false;

    protected final int CHANNEL_COUNT = 4;
    protected final int RANGE_COUNT = 5;

    protected final double[][] relativeBuffer = new double[RANGE_COUNT][CHANNEL_COUNT];
    protected boolean relativeStale = false;

    protected final boolean[] sensorsStateBuffer = new boolean[CHANNEL_COUNT];
    protected boolean sensorsStale = false;

    protected double batteryValue;
    protected boolean batteryStale = false;

    protected boolean isForeheadTouch = false;

    public void processMuseDataSensors(ArrayList<Double> packetValues) {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            sensorsStateBuffer[i] = packetValues.get(i) > 0.5;
        }
        sensorsStale = true;
    }

    public void processMuseDataBattery(MuseDataPacket p) {
        batteryValue = p.getBatteryValue(Battery.CHARGE_PERCENTAGE_REMAINING);
        batteryStale = true;
    }

    public void processMuseDataRelative(ArrayList<Double> packetValues, int relativeIndex) {
        fillRelativeBufferWith(relativeIndex, packetValues);
        relativeStale = true;
    }

    private void fillRelativeBufferWith(final int rangeIndex, final ArrayList<Double> packetValues) {
        for (int i = 0; i < packetValues.size(); i++) {
            Double v = packetValues.get(i);
            relativeBuffer[rangeIndex][i] = v;
        }
    }

    public void processMuseArtifactPacket(final MuseArtifactPacket p) {
        isForeheadTouch = p.getHeadbandOn();
        sensorsStale = true;
    }

    public void updateConnectionStatus(ConnectionState current) {
        connectionState = current;
        isConnectionStatusStale = true;
    }
}
