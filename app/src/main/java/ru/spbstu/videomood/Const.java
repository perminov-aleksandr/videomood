package ru.spbstu.videomood;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Range;

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

}
