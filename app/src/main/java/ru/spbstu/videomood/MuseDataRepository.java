package ru.spbstu.videomood;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.choosemuse.libmuse.Battery;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import ru.spbstu.videomood.activities.MuseActivity;
import ru.spbstu.videomood.activities.VideoActivity;

import static ru.spbstu.videomood.Const.CHANNEL_COUNT;
import static ru.spbstu.videomood.Const.RANGE_COUNT;
import static ru.spbstu.videomood.MuseManager.connect;

public class MuseDataRepository {
    private Context context;

    private MuseMoodSolver museMoodSolver;

    public MuseDataRepository(Context context) {
        this.context = context;
        museMoodSolver = new MuseMoodSolver(this);
        init();
    }

    private void init() {
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
        }
    }

    private void registerListeners() {
        MuseManager.setContext(context);
        MuseManager.registerMuseListeners(connectionListener, dataListener);
    }

    private ConnectionState connectionState;
    protected ConnectionState getConnectionState() {
        return connectionState;
    }

    protected boolean isConnectionStatusStale = false;

    protected final double[][] relativeBuffer = new double[RANGE_COUNT][CHANNEL_COUNT];

    protected final boolean[] sensorsStateBuffer = new boolean[CHANNEL_COUNT];

    protected double batteryValue;

    protected boolean isForeheadTouch = false;

    public void processMuseDataSensors(ArrayList<Double> packetValues) {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            sensorsStateBuffer[i] = packetValues.get(i) > 0.5;
        }
    }

    public void processMuseDataBattery(MuseDataPacket p) {
        batteryValue = p.getBatteryValue(Battery.CHARGE_PERCENTAGE_REMAINING);
    }

    public void processMuseDataRelative(ArrayList<Double> packetValues, int relativeIndex) {
        fillRelativeBufferWith(relativeIndex, packetValues);
        recalculatePercent();
    }

    private final int timeArrayLength = 60*10;
    private final Queue<Long[]> percentTimeQueue = new ArrayDeque<>(timeArrayLength);

    private void recalculatePercent() {
        BarValues barValues = new BarValues().calculate(relativeBuffer);
        long alphaPercent = barValues.getAlphaPercent();
        long betaPercent = barValues.getBetaPercent();

        boolean isPanic = museMoodSolver.solve();

        museData.alphaPercent = Long.valueOf(alphaPercent).intValue();
        museData.betaPercent = Long.valueOf(betaPercent).intValue();
    }

    private void fillRelativeBufferWith(final int rangeIndex, final ArrayList<Double> packetValues) {
        for (int i = 0; i < packetValues.size(); i++) {
            Double v = packetValues.get(i);
            relativeBuffer[rangeIndex][i] = v;
        }
    }

    public void processMuseArtifactPacket(final MuseArtifactPacket p) {
        museData.isForeheadTouch = p.getHeadbandOn();
    }

    private MuseData museData;

    public LiveData<MuseData> getMuseData() {
        final MutableLiveData<MuseData> data = new MutableLiveData<>();
        data.setValue(museData);
        return data;
    }

    public void reconnect() {
        connect();
    }
}
