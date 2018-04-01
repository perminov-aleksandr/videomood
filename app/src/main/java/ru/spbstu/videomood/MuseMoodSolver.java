package ru.spbstu.videomood;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.choosemuse.libmuse.ConnectionState;

import java.util.ArrayDeque;
import java.util.Queue;

public class MuseMoodSolver {
    private static final String TAG = "VideoMood:Solver";

    public MuseMoodSolver() {

    }

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

    private long alphaPercentSum;
    private long betaPercentSum;

    private boolean checkIsWarning() {
        Log.i(TAG, String.format("warning check: (%d/%d), queue size is %d", alphaPercentSum, betaPercentSum, percentTimeQueue.size()));

        return betaPercentSum >= betaPercentToWarning;
    }

    private boolean checkIsCalm() {
        Log.i(TAG, String.format("calm check: (%d/%d), queue size is %d", alphaPercentSum, betaPercentSum, percentTimeQueue.size()));

        return alphaPercentSum >= alphaPercentToWarning;
    }

    private final int timeArrayLength = 60*10;
    private final Queue<Long[]> percentTimeQueue = new ArrayDeque<>(timeArrayLength);

    private boolean isPanic = false;

    private void switchToCalmCheck() {
        isPanic = true;
        percentTimeQueue.clear();
        //warningHandler.removeCallbacks(checkWarningRunnable);
        //calmHandler.postDelayed(checkCalmRunnable, checkCalmDelay);
    }

    private void switchToWarningCheck() {
        isPanic = false;
        percentTimeQueue.clear();
        //calmHandler.removeCallbacks(checkCalmRunnable);
        //warningHandler.postDelayed(checkWarningRunnable, checkWarningDelay);
    }

    private void calcPercentSum() {
        long inAlphaCount = 0;
        long inBetaCount = 0;
        for (Long[] percentArr : percentTimeQueue) {
            if (percentArr[Const.Rhythms.BETA] > percentArr[Const.Rhythms.ALPHA])
                inBetaCount++;
            else
                inAlphaCount++;
        }

        int countSum = percentTimeQueue.size();
        alphaPercentSum = (long)( 100.0 * (double)inAlphaCount / countSum ) ;
        betaPercentSum = (long)( 100.0 * (double)inBetaCount / countSum ) ;
    }

    public boolean solve(long alphaPercent, long betaPercent){
        percentTimeQueue.add(new Long[]{ alphaPercent, betaPercent });
        calcPercentSum();
        if (isPanic) {
            if (checkIsCalm()) {
                switchToWarningCheck();
            } else {
                switchToCalmCheck();
            }
        } else {
            if (checkIsWarning()) {
                switchToCalmCheck();
            } else {
                switchToWarningCheck();
            }
        }
        return isPanic;
    }
}
