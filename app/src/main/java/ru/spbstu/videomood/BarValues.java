package ru.spbstu.videomood;

public class BarValues {
    private int alphaPercent;
    private int betaPercent;

    int getAlphaPercent() {
        return alphaPercent;
    }

    int getBetaPercent() {
        return betaPercent;
    }

    /***
     * Calculate weighted values each of channel in relativeBuffer for ALPHA and BETA ranges.
     * @return new BarValues instance filled according to calculated values
     */
    BarValues calculate(double[][] relativeBuffer) {
        double alphaMean = Utils.mean(relativeBuffer[Const.Rhythms.ALPHA]);
        double betaMean = Utils.mean(relativeBuffer[Const.Rhythms.BETA]);

        double t = 100.0 / (alphaMean + betaMean);

        double alphaWeighted = alphaMean * t;
        double betaWeighted = betaMean * t;

        alphaPercent = (int) Math.round(alphaWeighted);
        betaPercent = (int) Math.round(betaWeighted);
        return this;
    }
}
