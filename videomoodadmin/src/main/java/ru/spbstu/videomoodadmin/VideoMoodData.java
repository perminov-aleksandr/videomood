package ru.spbstu.videomoodadmin;

import java.util.Queue;

import ru.spbstu.videomood.btservice.DataPacket;

public class VideoMoodData {
    private Queue<Integer> alphas;

    private DataPacket dataPacket;

    public void setDataPacket(DataPacket packet){
        this.dataPacket = packet.clone();
    }

    public void setAlphas(Queue<Integer> alphas) {
        this.alphas = alphas;
    }

    public DataPacket getDataPacket() {
        return dataPacket;
    }

    public Queue<Integer> getAlphas() {
        return alphas;
    }
}
