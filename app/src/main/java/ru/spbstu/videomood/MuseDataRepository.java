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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import static ru.spbstu.videomood.Const.CHANNEL_COUNT;
import static ru.spbstu.videomood.Const.RANGE_COUNT;

public class MuseDataRepository {
    private Context context;

    private MuseMoodSolver museMoodSolver;

    public MuseDataRepository(Context context) {
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
        fillRelativeBufferWith(relativeIndex, packetValues);
        calculatePercent();
    }

    private void calculatePercent() {
        BarValues barValues = new BarValues().calculate(relativeBuffer);
        long alphaPercent = barValues.getAlphaPercent();
        long betaPercent = barValues.getBetaPercent();

        museData.alphaPercent = Long.valueOf(alphaPercent).intValue();
        museData.betaPercent = Long.valueOf(betaPercent).intValue();
        museData.isPanic = museMoodSolver.solve(alphaPercent, betaPercent);
        setLiveMuseData();
    }

    private void fillRelativeBufferWith(final int rangeIndex, final ArrayList<Double> packetValues) {
        for (int i = 0; i < packetValues.size(); i++) {
            Double v = packetValues.get(i);
            relativeBuffer[rangeIndex][i] = v;
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

    public void connect() {
        MuseManager.connect();
    }
}
