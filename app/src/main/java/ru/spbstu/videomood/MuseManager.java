package ru.spbstu.videomood;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseManagerAndroid;

public class MuseManager extends DeviceManager {

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    public MuseManager() {

    }

    @Override
    DeviceType getDeviceType() {
        return DeviceType.MUSE;
    }

}
