package ru.spbstu.videomood;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class Const {
    public static final Mood[] moods = new Mood[]{
            Mood.GREAT,
            Mood.NORMAL,
            Mood.AWFUL
    };

    public static final class Electrodes{
        public static final int FIRST = 0;
        public static final int SECOND = 1;
        public static final int THIRD = 2;
        public static final int FOURTH = 3;
    }

    public static final class Rhythms{
        public static final int ALPHA = 0;
        public static final int BETA = 1;
        public static final int GAMMA = 2;
        public static final int DELTA = 3;
        public static final int THETA = 4;
    }

    public static final int SECOND = 1000;
    public static final int CHANNEL_COUNT = 4;
    public static final int RANGE_COUNT = 5;
}
