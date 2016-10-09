package ru.spbstu.videomood;

import java.util.HashMap;
import java.util.Map;

import android.util.Range;

import com.choosemuse.libmuse.MuseManagerAndroid;

public class MuseMoodSolver {

    private User user;

    private final int alphaIndex = 0;
    private final int betaIndex = 1;
    private final int gammaIndex = 2;
    private final int deltaIndex = 3;
    private final int thetaIndex = 4;

    private final double[] sessionScores = new double[5];

    private final Map<Range<Double>[], Mood> moodTable;

    public MuseMoodSolver(MuseManagerAndroid museManager, User user) {
        this.user = user;

        moodTable = new HashMap<>();
        Range<Double>[] key = new Range[]{
                new Range<>(0, 100), new Range<>(50, 60), new Range<>(15, 30), new Range<>(180, 200), new Range<>(50, 100)
        };
        moodTable.put(key, Mood.AWFUL);
        moodTable.put(new Range[]{
                new Range(0,100), new Range(60, 70), new Range(15,30), new Range(120,160), new Range(50,100)
        }, Mood.BAD);
        moodTable.put(new Range[]{
                new Range(100,200), new Range(70, 100), new Range(15,30), new Range(80, 120), new Range(50,100)
        }, Mood.NORMAL);
        moodTable.put(new Range[]{
                new Range(0,100), new Range(100, 110), new Range(30,45), new Range(40, 80), new Range(50,100)
        }, Mood.GOOD);
        moodTable.put(new Range[]{
                new Range(0,100), new Range(110, 120), new Range(30,45), new Range(20, 40), new Range(50,100)
        }, Mood.GREAT);
    }

    public Mood solve() {
        for (Range<Double>[] rangeArr : moodTable.keySet()) {
            boolean isInAllRanges = true;
            for (int i = 0; i < rangeArr.length; i++) {
                double rhythmScore = sessionScores[i];
                Range<Double> rhythmRange = rangeArr[i];
                if (rhythmRange != null && !rhythmRange.contains(rhythmScore)) {
                    isInAllRanges = false;
                    break;
                }
            }
            if (isInAllRanges)
                return moodTable.get(rangeArr);
        }

        return user.getCurrentMood();
    }
}
