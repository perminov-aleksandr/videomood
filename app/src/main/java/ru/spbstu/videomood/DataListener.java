package ru.spbstu.videomood;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ru.spbstu.videomood.activities.MuseActivity;
import ru.spbstu.videomood.activities.VideoActivity;

public class DataListener extends MuseDataListener {
    final WeakReference<MuseActivity> activityRef;

    public DataListener(final WeakReference<MuseActivity> activityRef) {
        this.activityRef = activityRef;
    }

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
        MuseActivity activity = activityRef.get();

        ArrayList<Double> packetValues = p.values();
        switch (p.packetType()) {
            case ALPHA_RELATIVE:
                activity.processMuseDataRelative(packetValues, Const.Rhythms.ALPHA);
                break;
            case BETA_RELATIVE:
                activity.processMuseDataRelative(packetValues, Const.Rhythms.BETA);
                break;
            case BATTERY:
                activity.processMuseDataBattery(p);
                break;
            case IS_GOOD:
                activity.processMuseDataSensors(packetValues);
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
        activityRef.get().processMuseArtifactPacket(p);
    }
}
