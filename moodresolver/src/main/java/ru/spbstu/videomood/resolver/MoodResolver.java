package ru.spbstu.videomood.resolver;

import java.util.HashMap;

public class MoodResolver {

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

    private static boolean isBlackPixel(float[] arr, int x, int y) {
        return arr[x] == y;
    }

    /// <summary>
    /// Box-counting algorithm
    /// </summary>
    /// <param name="bw">black-white bitmap</param>
    /// <param name="startSize">initial size of square of grid</param>
    /// <param name="finishSize">final size of square of grid</param>
    /// <param name="step">step of changing of the grid</param>
    /// <returns>baList.Add(Math.Log(1d/b), Math.Log(a)), where b is swuare length size, a is the number of intersection of image with grid squares</returns>
    public static HashMap<Double, Double> boxCountingDimension(float[] bwPlot, int startSize, int finishSize, int step)
    {
        //length size - number of boxes
        HashMap<Double, Double> baList = new HashMap<>();

        float minWidth = Float.MAX_VALUE;
        float maxWidth = 0f;
        for (int i = 0; i < bwPlot.length; i++) {
            if (maxWidth < bwPlot[i])
                maxWidth = bwPlot[i];
            if (minWidth > bwPlot[i])
                minWidth = bwPlot[i];
        }

        int bwWidth = Math.round(maxWidth) - Math.round(minWidth);
        int bwHeight = bwPlot.length;

        for (int b = startSize; b <= finishSize; b += step)
        {
            int hCount = bwHeight/b;
            int wCount = bwWidth/b;
            boolean[][] filledBoxes = new boolean[wCount + (bwWidth > wCount*b ? 1 : 0)][hCount + (bwHeight > hCount*b ? 1 : 0)];

            for (int x = 0; x < bwWidth; x++)
            {
                for (int y = 0; y < bwHeight; y++)
                {
                    if (isBlackPixel(bwPlot, x, y))
                    {
                        int xBox = x/b;
                        int yBox = y/b;
                        filledBoxes[xBox][yBox] = true;
                    }
                }
            }

            int a = 0;
            for (int i = 0; i < filledBoxes.length; i++)
            {
                for (int j = 0; j < filledBoxes[0].length; j++)
                {
                    if (filledBoxes[i][j])
                    {
                        a++;
                    }
                }
            }

            baList.put(Math.log(1d/b), Math.log(a));

            /*if (dataPath.Length > 0)
            {
                if (dataPath[dataPath.Length - 1] != '\\')
                {
                    dataPath += '\\';
                }
                if (Directory.Exists(dataPath))
                {
                    XBitmap bmp = new XBitmap(bw);
                    for (int i = 0; i < filledBoxes.GetLength(0); i++)
                    {
                        bmp.DrawLine(i * b, 0, i * b, bmp.Height, Color.HotPink);
                    }
                    for (int j = 0; j < filledBoxes.GetLength(1); j++)
                    {
                        bmp.DrawLine(0, j * b, bmp.Width, j * b, Color.HotPink);
                    }
                    for (int i = 0; i < filledBoxes.GetLength(0); i++)
                    {
                        for (int j = 0; j < filledBoxes.GetLength(1); j++)
                        {
                            if (filledBoxes[i, j])
                            {
                                bmp.FillRectangle(i * b, j * b, i * b + b, j * b + b, Color.Red, 2);
                            }
                        }
                    }
                    bmp.ConvertToNativeBitmap().Save(dataPath + b + ".bmp");
                }
            }

            Logger.Instance.Log("BoxCounting: b is " + b + " of " + finishSize);*/

        }

        /*if (dataPath.Length > 0)
        {
            using (StreamWriter sw = new StreamWriter(dataPath + "ba.csv"))
            {
                sw.WriteLine("NumberOfBoxes,LengthOfSideInv");
                foreach (double bInv in baList.Keys)
                {
                    sw.WriteLine(baList[bInv] + "," + bInv);
                }
                sw.Close();
            }
        }*/

        return baList;
    }

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
