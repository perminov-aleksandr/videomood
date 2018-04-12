package ru.spbstu.videomood.btservice;

import java.util.ArrayList;

public class VideosPacket extends Packet {

    private ArrayList<VideoItem> videoList;

    public ArrayList<VideoItem> getVideoList() {
        return videoList;
    }

    public void setVideoList(ArrayList<VideoItem> videoList) {
        this.videoList = videoList;
    }

    public PacketType type() {
        return PacketType.VIDEOS;
    }
}
