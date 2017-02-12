package ru.spbstu.videomood.btservice;

import java.util.ArrayList;

public class DataPacket extends Packet {
    //private long timestamp;

    private Boolean museState;

    private Integer museBatteryPercent;

    private Boolean[] museSensorsState;

    private Integer alphaPct;

    private Integer betaPct;

    private Integer headsetBatteryPercent;

    private String videoName;

    private Boolean videoState;

    private ArrayList<VideoItem> videoList;

    /*public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }*/

    public Boolean getMuseState() {
        return museState;
    }

    public void setMuseState(boolean museState) {
        this.museState = museState;
    }

    public Integer getMuseBatteryPercent() {
        return museBatteryPercent;
    }

    public void setMuseBatteryPercent(Integer museBatteryPercent) {
        this.museBatteryPercent = museBatteryPercent;
    }

    public Boolean[] getMuseSensorsState() {
        return museSensorsState;
    }

    public void setMuseSensorsState(Boolean[] museSensorsState) {
        this.museSensorsState = museSensorsState;
    }

    public Integer getAlphaPct() {
        return alphaPct;
    }

    public void setAlphaPct(Integer alphaPct) {
        this.alphaPct = alphaPct;
    }

    public Integer getBetaPct() {
        return betaPct;
    }

    public void setBetaPct(Integer betaPct) {
        this.betaPct = betaPct;
    }

    public Integer getHeadsetBatteryPercent() {
        return headsetBatteryPercent;
    }

    public void setHeadsetBatteryPercent(Integer headsetBatteryPercent) {
        this.headsetBatteryPercent = headsetBatteryPercent;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public Boolean getVideoState() {
        return videoState;
    }

    public void setVideoState(boolean videoState) {
        this.videoState = videoState;
    }

    public ArrayList<VideoItem> getVideoList() {
        return videoList;
    }

    public void setVideoList(ArrayList<VideoItem> videoList) {
        this.videoList = videoList;
    }

    @Override
    public PacketType type() {
        return PacketType.DATA;
    }
}
