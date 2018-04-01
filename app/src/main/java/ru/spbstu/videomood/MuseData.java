package ru.spbstu.videomood;

import com.choosemuse.libmuse.ConnectionState;

public class MuseData {
    public ConnectionState connectionState;

    public boolean[] sensorsStateBuffer = new boolean[Const.CHANNEL_COUNT];
    public Boolean isForeheadTouch;
    public Integer batteryPercent;

    public Integer alphaPercent;
    public Integer betaPercent;

    public Boolean isPanic;
}
