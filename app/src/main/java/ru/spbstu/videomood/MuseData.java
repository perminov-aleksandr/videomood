package ru.spbstu.videomood;

import com.choosemuse.libmuse.ConnectionState;

public class MuseData {
    public ConnectionState connectionState;

    public boolean[] sensorsStateBuffer = new boolean[Const.CHANNEL_COUNT];
    public boolean isForeheadTouch;
    public int batteryPercent;

    public int alphaPercent;
    public int betaPercent;

    public boolean isPanic;
}
