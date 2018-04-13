package ru.spbstu.videomood;

import android.content.Context;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.util.ArrayList;

public final class MuseManager {

    private static boolean isPermissionGranted = false;

    private MuseManager() {
    }

    private static MuseManagerAndroid manager = MuseManagerAndroid.getInstance();

    public static MuseManagerAndroid getManager() {
        return manager;
    }

    public static void setContext(Context context) {
        manager.setContext(context);
    }

    public static void startListening() {
        if (isPermissionGranted)
            manager.startListening();
    }

    public static void stopListening() {
        manager.stopListening();
    }

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private static Muse muse;

    public static Muse getMuse() {
        return muse;
    }

    public static void setMuse(Muse m) {
        muse = m;
    }

    // Unregister all prior listeners and register our data listener to
    // receive the MuseDataPacketTypes we are interested in.  If you do
    // not register a listener for a particular data type, you will not
    // receive data packets of that type.
    public static void registerMuseListeners(MuseConnectionListener connectionListener, MuseDataListener dataListener) {
        muse.unregisterAllListeners();
        muse.registerConnectionListener(connectionListener);

        MuseDataPacketType[] processedPacketTypes = new MuseDataPacketType[]{
                MuseDataPacketType.ALPHA_RELATIVE,
                MuseDataPacketType.BETA_RELATIVE,
                MuseDataPacketType.IS_GOOD,
                MuseDataPacketType.ARTIFACTS,
                MuseDataPacketType.BATTERY
        };

        for (int i = 0; i < processedPacketTypes.length; i++) {
            muse.registerDataListener(dataListener, processedPacketTypes[i]);
        }
    }

    public static ArrayList<Muse> getMuses() {
        return manager.getMuses();
    }

    public static void enableDataTransmission() {
        if (muse != null)
            muse.enableDataTransmission(true);
    }

    public static void disableDataTransmission() {
        if (muse != null)
            muse.enableDataTransmission(false);
    }

    public static void disconnect() {
        if (muse != null) {
            muse.unregisterAllListeners();
            muse.disconnect(false);
        }
    }

    public static void connect() {
        muse.runAsynchronously();
    }

    public static void setPermissionGranted() {
        isPermissionGranted = true;
    }
}
