package ru.spbstu.videomood;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Const {
    public static final String selectedMuseIndexStr = "selectedMuseIndex";
    public static final String ageRangeIndexStr = "ageRangeIndex";
    public static final String moodStr = "mood";

    public static final Range<Integer>[] ageRanges = new Range[]{
            new Range(0,6),
            new Range(6,12),
            new Range(12,16),
            new Range(16,25),
            new Range(25,99),
    };

    public static final Mood[] moods = new Mood[]{
            Mood.GREAT,
            Mood.NORMAL,
            Mood.AWFUL
    };

    //electrodes
    public static final int FIRST = 0;
    public static final int SECOND = 1;
    public static final int THIRD = 2;
    public static final int FOURTH = 3;

    //rhythms
    public static final int ALPHA = 0;
    public static final int BETA = 1;
    public static final int GAMMA = 2;
    public static final int DELTA = 3;
    public static final int THETA = 4;
}
