package ru.spbstu.videomood;

import android.util.Log;

import java.util.ArrayList;

public class MuseMoodSolver {
    private static final String TAG = "VideoMood:Solver";

    private final long second = 1000;

    private long checkCalmDelay = 10 * second;
    private long checkWarningDelay = 90 * second;
    private long checkWarningPeriod = second;
    private long checkCalmPeriod = second;

    //private Handler warningHandler = new Handler();

    //private Handler calmHandler = new Handler();

    /*private final Runnable checkWarningRunnable = new Runnable() {
        @Override
        public void run() {
            calcPercentSum();
            //check if we should interrupt video and ask to calm down
            if (checkIsWarning()) {
                switchToCalmCheck();
            } else {
                //or we could continue watching and counting
                warningHandler.postDelayed(this, checkWarningPeriod);
            }
        }
    };

    private final Runnable checkCalmRunnable = new Runnable() {
        @Override
        public void run() {
            switchToWarningCheck();
        }
    };*/

    public static final int betaPercentToWarning = 20;
    public static final int alphaPercentToWarning = 100-betaPercentToWarning;

    private int alphaPercentSum;
    private int betaPercentSum;

    private boolean checkIsWarning() {
        Log.i(TAG, String.format("warning check: (%d/%d)", alphaPercentSum, betaPercentSum));

        return betaPercentSum >= betaPercentToWarning;
    }

    private boolean checkIsCalm() {
        Log.i(TAG, String.format("calm check: (%d/%d)", alphaPercentSum, betaPercentSum));

        return alphaPercentSum >= alphaPercentToWarning;
    }

    public static final int TimelineLength = 60*10;
    private final ArrayList<Long[]> percentTimeline = new ArrayList<>(TimelineLength);

    private boolean isPanic = false;

    private void switchToCalmCheck() {
        isPanic = true;
        percentTimeline.clear();
        //warningHandler.removeCallbacks(checkWarningRunnable);
        //calmHandler.postDelayed(checkCalmRunnable, checkCalmDelay);
    }

    private void switchToWarningCheck() {
        isPanic = false;
        percentTimeline.clear();
        //calmHandler.removeCallbacks(checkCalmRunnable);
        //warningHandler.postDelayed(checkWarningRunnable, checkWarningDelay);
    }

    private void calcPercentSum() {
        long inAlphaCount = 0;
        long inBetaCount = 0;
        for (Long[] percentArr : percentTimeline) {
            if (percentArr[Const.Rhythms.BETA] > percentArr[Const.Rhythms.ALPHA])
                inBetaCount++;
            else
                inAlphaCount++;
        }

        int countSum = percentTimeline.size();
        alphaPercentSum = (int)( 100.0 * (double)inAlphaCount / countSum ) ;
        betaPercentSum = (int)( 100.0 * (double)inBetaCount / countSum ) ;
    }

    private void pushPercentTimelineValue(long alphaPercent, long betaPercent) {
        Long[] alphaBetaPercent = new Long[2];
        alphaBetaPercent[Const.Rhythms.ALPHA] = alphaPercent;
        alphaBetaPercent[Const.Rhythms.BETA] = betaPercent;
        percentTimeline.add(alphaBetaPercent);
    }

    private boolean checkPercentTimelineFilled() {
        if (percentTimeline.size() < TimelineLength) {
            Log.d(TAG, String.format("Queue size %d/%d is not enough to solve", percentTimeline.size(), TimelineLength));
            return false;
        } else if (percentTimeline.size() == TimelineLength) {
            percentTimeline.remove(0);
            return true;
        }
        return false;
    }

    public boolean solve(long alphaPercent, long betaPercent){
        pushPercentTimelineValue(alphaPercent, betaPercent);
        if (!checkPercentTimelineFilled())
            return isPanic;

        calcPercentSum();

        if (isPanic) {
            if (checkIsCalm()) {
                switchToWarningCheck();
            }
        } else {
            if (checkIsWarning()) {
                switchToCalmCheck();
            }
        }
        return isPanic;
    }

    public int getAlphaPercentSum() {
        return alphaPercentSum;
    }

    public int getBetaPercentSum() {
        return betaPercentSum;
    }
}
