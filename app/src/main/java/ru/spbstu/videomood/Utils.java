package ru.spbstu.videomood;

public final class Utils {

    /***
     * Calculate mean value for array of values. Ignore NaN and Infinite values
     * @param arr - Array of values
     * @return mean value
     */
    public static double mean(double[] arr) {
        double res = 0.0;
        int count = 0;
        for (double value : arr) {
            if (Double.isNaN(value) || Double.isInfinite(value))
                continue;

            res += value;
            count++;
        }
        return res / count;
    }
}
