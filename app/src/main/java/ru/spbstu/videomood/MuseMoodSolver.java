package ru.spbstu.videomood;

import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.os.Build;

public class MuseMoodSolver {

    private User user;

    public User getUser() {
        return user;
    }

    public final int alphaIndex = 0;
    public final int betaIndex = 1;
    public final int gammaIndex = 2;
    public final int deltaIndex = 3;
    public final int thetaIndex = 4;

    private final Map<Range<Double>[], Mood> moodTable;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MuseMoodSolver( User user) {
        this.user = user;

        moodTable = new HashMap<>();

        moodTable.put(new Range[]{
                new Range<Double>(0.0, 100.0), new Range<Double>(50.0, 60.0), new Range<Double>(15.0, 30.0), new Range<Double>(180.0, 200.0), new Range<Double>(50.0, 100.0)
        }, Mood.AWFUL);
        moodTable.put(new Range[]{
                new Range<Double>(0.0,100.0), new Range<Double>(60.0, 70.0), new Range<Double>(15.0,30.0), new Range<Double>(120.0,160.0), new Range<Double>(50.0,100.0)
        }, Mood.BAD);
        moodTable.put(new Range[]{
                new Range<Double>(100.0,200.0), new Range<Double>(70.0, 100.0), new Range<Double>(15.0,30.0), new Range<Double>(80.0, 120.0), new Range<Double>(50.0,100.0)
        }, Mood.NORMAL);
        moodTable.put(new Range[]{
                new Range<Double>(0.0,100.0), new Range<Double>(100.0, 110.0), new Range<Double>(30.0,45.0), new Range<Double>(40.0, 80.0), new Range<Double>(50.0,100.0)
        }, Mood.GOOD);
        moodTable.put(new Range[]{
                new Range<Double>(0.0,100.0), new Range<Double>(110.0, 120.0), new Range<Double>(30.0,45.0), new Range<Double>(20.0, 40.0), new Range<Double>(50.0,100.0)
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
