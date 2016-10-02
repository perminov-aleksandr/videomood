package ru.spbstu.videomood;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseManagerAndroid;

public class MuseManager extends DeviceManager {

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid manager;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    @Override
    DeviceType getDeviceType() {
        return DeviceType.MUSE;
    }


}
