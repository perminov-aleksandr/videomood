package ru.spbstu.videomoodadmin;

import java.util.Queue;

import ru.spbstu.videomood.btservice.VideoActivityState;

public class VideoMoodData {
    private Queue<Integer> alphas;

    private VideoActivityState videoActivityState;

    public void setAlphas(Queue<Integer> alphas) {
        this.alphas = alphas;
    }

    public VideoActivityState getVideoActivityState() {
        return videoActivityState;
    }

    public Queue<Integer> getAlphas() {
        return alphas;
    }
}
