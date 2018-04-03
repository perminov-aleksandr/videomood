package ru.spbstu.videomood;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestMuseMoodSolver {
    @Test
    public void singleSolveTest() {
        MuseMoodSolver solver = new MuseMoodSolver();
        boolean isPanic = solver.solve(81, 19);
        assertEquals(isPanic, false);
    }

    @Test
    public void simpleSolveTest_panic() {
        MuseMoodSolver solver = new MuseMoodSolver();
        boolean resultIsPanic = false;
        int betaPercent = 51;
        int alphaPercent = 100-betaPercent;
        for (int i = 0; i < MuseMoodSolver.TimelineLength; i++) {
            resultIsPanic |= solver.solve(alphaPercent, betaPercent);
        }
        assertEquals(true, resultIsPanic);
    }
}
