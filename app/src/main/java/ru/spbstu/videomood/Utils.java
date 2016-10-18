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
        for (int i = 0; i < arr.length; i++) {
            double value = arr[i];
            if (Double.isNaN(value) || Double.isInfinite(value)) continue;

            res += value;
            count++;
        }
        return res / count;
    }
}
