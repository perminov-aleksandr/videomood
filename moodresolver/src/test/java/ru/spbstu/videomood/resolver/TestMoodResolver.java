package ru.spbstu.videomood.resolver;

import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseFileFactory;
import com.choosemuse.libmuse.MuseFileReader;
import com.choosemuse.libmuse.ReaderMuse;
import com.choosemuse.libmuse.ReaderMuseBuilder;
import com.choosemuse.libmuse.ReaderMusePlaybackSettings;
import com.choosemuse.libmuse.ReaderPlaybackListener;

import org.junit.Test;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class TestMoodResolver {

    private static final int SIGNAL_FREQUENCY_HZ = 220;
    private static final int SECONDS_TO_ANALYZE = 10;
    private static final int SAMPLES_SIZE = SIGNAL_FREQUENCY_HZ * SECONDS_TO_ANALYZE;
    private static final int QUEUE_SIZE = SAMPLES_SIZE + SIGNAL_FREQUENCY_HZ;

    private final Queue<Float> eeg2Queue = new ArrayDeque<>(QUEUE_SIZE);
    private final Queue<Float> eeg3Queue = new ArrayDeque<>(QUEUE_SIZE);

    private final Float[] eeg2Array = new Float[QUEUE_SIZE];
    private final Float[] eeg3Array = new Float[QUEUE_SIZE];

    private boolean isQueueReady = false;

    private Muse muse;

    private void setup(String path) {
        ReaderMuseBuilder rb = ReaderMuseBuilder.get();
        MuseFileReader fileReader = MuseFileFactory.getMuseFileReader(new File(path));
        ReaderMuse reader = rb.build(fileReader);
        muse = reader.asMuse();
        muse.registerDataListener(new MuseDataListener() {
            @Override
            public void receiveMuseDataPacket(MuseDataPacket museDataPacket, Muse muse) {
                float eeg2Value = (float)museDataPacket.getEegChannelValue(Eeg.EEG2);
                float eeg3Value = (float)museDataPacket.getEegChannelValue(Eeg.EEG3);
                eeg2Queue.add(eeg2Value);
                eeg3Queue.add(eeg3Value);
                if (eeg2Queue.size() >= SAMPLES_SIZE || eeg3Queue.size() >= SAMPLES_SIZE)
                    isQueueReady = true;
            }

            @Override
            public void receiveMuseArtifactPacket(MuseArtifactPacket museArtifactPacket, Muse muse) {}
        }, MuseDataPacketType.EEG);

        reader.setPlaybackListener(new ReaderPlaybackListener() {
            @Override
            public void receivePlaybackDone() {
                muse.unregisterAllListeners();
                analyzeTimer.cancel();
                isTestReady = true;
            }

            @Override
            public void receivePlaybackInterrupted() {}
        });
    }

    private Timer analyzeTimer;

    private boolean isTestReady = false;

    private class QueueAnalyzer extends TimerTask {
        private int stepSize;
        private int startSize;
        private int finishSize;

        public QueueAnalyzer(int stepSize, int startSize, int finishSize) {
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
            for (int i = 0; i < boxedValues.length; i++)
                unboxedValues[i] = Float.valueOf(boxedValues[i]);

            MoodResolver.getThetaValues(unboxedValues, startSize, finishSize, stepSize);

            for (int i = 0; i < SIGNAL_FREQUENCY_HZ; i++) {
                eeg3Queue.remove();
                eeg2Queue.remove();
            }

            isQueueReady = false;
        }
    }

    @Test
    public void test() {
        setup("museSampleFiles/2_wear.muse");
        muse.enableDataTransmission(true);

        analyzeTimer = new Timer();
        analyzeTimer.schedule(new QueueAnalyzer(100, 10, 1700), 100);

        while (!isTestReady);
    }
}
