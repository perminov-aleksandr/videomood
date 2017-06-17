package ru.spbstu.videomood.resolver;

import java.util.HashMap;

public class MoodResolver {

    public static int minY = 0;
    public static int maxY = 1700;

    public static double[] normalEquations2d(double[] y, double[] x) {
        // x^t * x
        double[][] xtx = new double[2][2];
        for (int i = 0; (i < x.length); i++) {
            xtx[0][1] = (xtx[0][ 1] + x[i]);
            xtx[0][0] = (xtx[0][ 0] + (x[i] * x[i]));
        }

        xtx[1][0] = xtx[0][1];
        xtx[1][1] = x.length;

        // inverse
        double[][] xtxInv = new double[2][2];
        double d = (1 / ((xtx[0][0] * xtx[1][1]) - (xtx[1][0] * xtx[0][1])));
        xtxInv[0][0] = (xtx[1][1] * d);
        xtxInv[0][1] = ((xtx[0][1] * d) * -1);
        xtxInv[1][0] = ((xtx[1][0] * d) * -1);
        xtxInv[1][1] = (xtx[0][0] * d);

        // times x^t
        double[][] xtxInvxt = new double[2][x.length];
        for (int i = 0; (i < 2); i++) {
            for (int j = 0; (j < x.length); j++) {
                xtxInvxt[i][j] = ((xtxInv[i][0] * x[j]) + xtxInv[i][1]);
            }
        }

        // times y
        double[] theta = new double[2];
        for (int i = 0; (i < 2); i++) {
            for (int j = 0; (j < x.length); j++) {
                theta[i] = (theta[i] + (xtxInvxt[i][j] * y[j]));
            }
        }

        return theta;
    }

    private static boolean isInRange(float value, int left, int right) {
        return value > left && value <= right;
    }

    /**
     * Box-counting algorithm
     * @param plot - timeline of values
     * @param startSize - initial size of square of grid
     * @param finishSize - final size of square of grid
     * @param step - step of changing of the grid
     * @return map Math.Log(1/b) to Math.Log(a) where b is square length size, a is the number of intersection of image with grid squares
     */
    public static HashMap<Double, Double> boxCountingDimension(float[] plot, int startSize, int finishSize, int step)
    {
        //length size - number of boxes
        HashMap<Double, Double> baList = new HashMap<>();

        int bwHeight = maxY - minY;

        for (int boxSize = startSize; boxSize <= finishSize; boxSize += step)
        {
            boolean[][] filledBoxes = fillBoxes(plot, boxSize, bwHeight);

            int a = 0;
            for (int i = 0; i < filledBoxes.length; i++)
                for (int j = 0; j < filledBoxes[0].length; j++)
                    if (filledBoxes[i][j])
                        a++;

            baList.put(Math.log(1d/boxSize), Math.log(a));
        }

        return baList;
    }

    /**
     * create array of boxes depend on timeline plot size and box size, mark boxes where values of plot are presented
     * @param plot - array of timeline plot values
     * @param boxSize - size of box to divide plot
     * @param plotHeight - plot height
     * @return array of marked boxes
     */
    private static boolean[][] fillBoxes(float[] plot, int boxSize, int plotHeight) {
        int plotWidth = plot.length;

        int hCount = plotHeight/boxSize;
        int wCount = plotWidth/boxSize;

        if (plotWidth > wCount*boxSize)
            wCount += 1;
        if (plotHeight > hCount*boxSize)
            hCount += 1;

        boolean[][] filledBoxes = new boolean[wCount][hCount];
        for (int i = 0; i < plot.length; i++) {
            float value = plot[i];
            int yBox = (int) (value/boxSize);
            int left = yBox*boxSize;
            int right = left + boxSize;
            if (isInRange(value, left, right)) {
                int xBox = i/boxSize;
                filledBoxes[xBox][yBox] = true;
            }
        }
        return filledBoxes;
    }

    /**
     *
     * @param curve
     * @param startSize
     * @param endSize
     * @param step
     * @return vector of two values: first value is angular coeff (Minkowski–Bouligand dimension) while second value — offset.
     */
    public static double[] getThetaValues(float[] curve, int startSize, int endSize, int step) {
        HashMap<Double, Double> baList = boxCountingDimension(curve, startSize, endSize, step);
        double[] y = new double[baList.size()];
        double[] x = new double[baList.size()];
        int c = 0;
        for (double key : baList.keySet())
        {
            y[c] = baList.get(key);
            x[c] = key;
            c++;
        }
        double[] theta = normalEquations2d(y, x);
        return theta;
    }
}
