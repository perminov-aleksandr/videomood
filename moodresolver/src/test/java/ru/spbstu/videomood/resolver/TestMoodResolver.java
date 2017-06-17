package ru.spbstu.videomood.resolver;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class TestMoodResolver {

    private static final int SIGNAL_FREQUENCY_HZ = 220;
    private static final int SECONDS_TO_ANALYZE = 1;
    private static final int SAMPLES_SIZE = SIGNAL_FREQUENCY_HZ * SECONDS_TO_ANALYZE;
    private static final int QUEUE_SIZE = SAMPLES_SIZE /*+ SIGNAL_FREQUENCY_HZ*/;

    private final Queue<Float> eeg2Queue = new ArrayDeque<>(QUEUE_SIZE);
    private final Queue<Float> eeg3Queue = new ArrayDeque<>(QUEUE_SIZE);

    private final Float[] eeg2Array = new Float[QUEUE_SIZE];
    private final Float[] eeg3Array = new Float[QUEUE_SIZE];

    private boolean isFileEnd = false;

    private Timer fileReaderTimer = new Timer();
    private boolean isQueueReady = false;

    private Timer analyzeTimer = new Timer();
    private boolean isTestReady = false;

    private class QueueAnalyzer extends TimerTask {
        private int stepSize;
        private int startSize;
        private int finishSize;

        QueueAnalyzer(int stepSize, int startSize, int finishSize) {
            this.stepSize = stepSize;
            this.startSize = startSize;
            this.finishSize = finishSize;
        }

        @Override
        public void run() {
            if (!isQueueReady)
                return;

            Float[] boxedValues = eeg2Queue.toArray(eeg2Array);
            float[] unboxedValues = new float[boxedValues.length];
            for (int i = 0; i < boxedValues.length; i++) {
                if (boxedValues[i] != null)
                    unboxedValues[i] = boxedValues[i];
            }


            double[] thetaValues = MoodResolver.getThetaValues(unboxedValues, startSize, finishSize, stepSize);

            for (int i = 0; i < SIGNAL_FREQUENCY_HZ; i++) {
                eeg3Queue.remove();
                eeg2Queue.remove();
            }

            if (isFileEnd) {
                isTestReady = true;
                this.cancel();
            }
            else
                isQueueReady = false;
        }
    }

    @Test
    public void test() {
        try {
            String filePath = "museSampleFiles/2_wear.csv";

            FileReaderTask fileReaderTask = new FileReaderTask(filePath);
            fileReaderTimer.schedule(fileReaderTask, 0, 20);

            QueueAnalyzer queueAnalyze = new QueueAnalyzer(100, 10, 1700);
            analyzeTimer.schedule(queueAnalyze, 0, 200);

            while (!isTestReady)
                Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class FileReaderTask extends TimerTask {

        private BufferedReader br;

        FileReaderTask(String path) throws FileNotFoundException {
            br = new BufferedReader(new FileReader(path));
        }

        @Override
        public void run() {
            float eeg2Value;
            float eeg3Value;

            try {
                String line = br.readLine();
                if (line == null) {
                    isFileEnd = true;
                    this.cancel();
                    return;
                }

                String[] csValues = line.split(",");
                if (csValues.length < 6 || !csValues[1].equals(" /muse/eeg"))
                    return;

                eeg2Value = Float.parseFloat(csValues[4]);
                eeg3Value = Float.parseFloat(csValues[4]);
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
                return;
            }

            eeg2Queue.add(eeg2Value);
            eeg3Queue.add(eeg3Value);
            if (eeg2Queue.size() >= SAMPLES_SIZE || eeg3Queue.size() >= SAMPLES_SIZE)
                isQueueReady = true;
        }

        @Override
        public boolean cancel() {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return super.cancel();
        }
    }
}
