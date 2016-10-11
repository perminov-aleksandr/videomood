package ru.spbstu.videomood;

import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Range;

public class MuseMoodSolver {

    private User user;

    public User getUser() {
        return user;
    }

    private final int alphaIndex = 0;
    private final int betaIndex = 1;
    private final int gammaIndex = 2;
    private final int deltaIndex = 3;
    private final int thetaIndex = 4;

    private final Map<Range<Double>[], Mood> moodTable;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MuseMoodSolver( User user) {
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

    public void solve(double[] sessionScores) {
        Mood mood = null;
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
            if (isInAllRanges) {
                mood = moodTable.get(rangeArr);
                break;
            }
        }

        if (mood != null)
            user.setCurrentMood(mood);
    }
}
