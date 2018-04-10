package ru.spbstu.videomood;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;

import com.choosemuse.libmuse.Battery;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import static ru.spbstu.videomood.Const.CHANNEL_COUNT;
import static ru.spbstu.videomood.Const.RANGE_COUNT;

public final class MuseDataRepository implements LifecycleObserver {

    private static final String TAG = "MuseDataRepository";
    private Context context;

    private MuseMoodSolver museMoodSolver;

    private static MuseDataRepository instance = null;

    public static MuseDataRepository getInstance(Context context) {
        if (instance == null)
            instance = new MuseDataRepository(context);

        return instance;
    }

    private MuseDataRepository(Context context) {
        this.context = context;
        museMoodSolver = new MuseMoodSolver();
        registerListeners();
        connect();
    }

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     * <p>
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener = new DataListener();

    class DataListener extends MuseDataListener {
        /**
         * You will receive a callback to this method each time the headband sends a MuseDataPacket
         * that you have registered.  You can use different listeners for different packet types or
         * a single listener for all packet types as we have done here.
         *
         * @param p    The data packet containing the data from the headband (eg. EEG data)
         * @param muse The headband that sent the information.
         */
        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            ArrayList<Double> packetValues = p.values();
            /*String packetValuesString = packetValues == null ? "<None>" : (String.format("%s,%s,%s,%s", packetValues.get(0), packetValues.get(1), packetValues.get(2), packetValues.get(3)));
            Log.d(TAG, String.format("Muse packet received %s with values %s", p.packetType().name(), packetValuesString));*/
            switch (p.packetType()) {
                case ALPHA_RELATIVE:
                    processMuseDataRelative(packetValues, Const.Rhythms.ALPHA);
                    break;
                case BETA_RELATIVE:
                    processMuseDataRelative(packetValues, Const.Rhythms.BETA);
                    break;
                case BATTERY:
                    processMuseDataBattery(p);
                    break;
                case IS_GOOD:
                    processMuseDataSensors(packetValues);
                    break;
                default:
                    break;
            }
        }

        /**
         * You will receive a callback to this method each time an artifact packet is generated if you
         * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
         * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
         *
         * @param p    The artifact packet with the data from the headband.
         * @param muse The headband that sent the information.
         */
        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            processMuseArtifactPacket(p);
        }
    }

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection connectionState of a headband, for example when the headband connects
     * or disconnects.
     * <p>
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener = new ConnectionListener();

    class ConnectionListener extends MuseConnectionListener {
        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            final ConnectionState current = p.getCurrentConnectionState();
            museData.connectionState = current;
            setLiveMuseData();
        }
    }

    private void registerListeners() {
        MuseManager.setContext(context);
        MuseManager.registerMuseListeners(connectionListener, dataListener);
    }

    private final double[][] relativeBuffer = new double[RANGE_COUNT][CHANNEL_COUNT];

    private void processMuseDataSensors(ArrayList<Double> packetValues) {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            museData.sensorsStateBuffer[i] = packetValues.get(i) > 0.5;
        }
        setLiveMuseData();
    }

    private void processMuseDataBattery(MuseDataPacket p) {
        museData.batteryPercent = (int) p.getBatteryValue(Battery.CHARGE_PERCENTAGE_REMAINING);
        setLiveMuseData();
    }

    private void processMuseDataRelative(ArrayList<Double> packetValues, int relativeIndex) {
        if (museData.isForeheadTouch) {
            fillRelativeBufferWith(relativeIndex, packetValues);
            calculatePercent();
        }
    }

    private void calculatePercent() {
        BarValues barValues = new BarValues().calculate(relativeBuffer);
        int alphaPercent = barValues.getAlphaPercent();
        int betaPercent = barValues.getBetaPercent();

        museData.isPanic = museMoodSolver.solve(alphaPercent, betaPercent);
        museData.alphaPercent = alphaPercent;
        museData.betaPercent = betaPercent;
        setLiveMuseData();
    }

    private void fillRelativeBufferWith(final int relativeIndex, final ArrayList<Double> packetValues) {
        for (int i = 0; i < packetValues.size(); i++) {
            Double v = packetValues.get(i);
            relativeBuffer[relativeIndex][i] = v;
        }
    }

    private void processMuseArtifactPacket(final MuseArtifactPacket p) {
        museData.isForeheadTouch = p.getHeadbandOn();
        setLiveMuseData();
    }

    private MuseData museData = new MuseData();

    private MutableLiveData<MuseData> liveMuseData = new MutableLiveData<>();

    private void setLiveMuseData() {
        liveMuseData.postValue(museData);
    }

    public LiveData<MuseData> getMuseData() {
        return liveMuseData;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onStart() {
        connect();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onStop() {
        disconnect();
    }

    public void connect() {
        MuseManager.connect();
    }

    public void disconnect() {
        MuseManager.disconnect();
    }
}
