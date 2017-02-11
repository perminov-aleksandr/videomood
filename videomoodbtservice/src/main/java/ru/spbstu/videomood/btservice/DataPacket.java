package ru.spbstu.videomood.btservice;

import java.util.ArrayList;

public class DataPacket extends Packet implements Cloneable {
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

    public int getMuseBatteryPercent() {
        return museBatteryPercent;
    }

    public void setMuseBatteryPercent(int museBatteryPercent) {
        this.museBatteryPercent = museBatteryPercent;
    }

    public Boolean[] getMuseSensorsState() {
        return museSensorsState;
    }

    public void setMuseSensorsState(Boolean[] museSensorsState) {
        this.museSensorsState = museSensorsState;
    }

    public int getAlphaPct() {
        return alphaPct;
    }

    public void setAlphaPct(int alphaPct) {
        this.alphaPct = alphaPct;
    }

    public int getBetaPct() {
        return betaPct;
    }

    public void setBetaPct(int betaPct) {
        this.betaPct = betaPct;
    }

    public int getHeadsetBatteryPercent() {
        return headsetBatteryPercent;
    }

    public void setHeadsetBatteryPercent(int headsetBatteryPercent) {
        this.headsetBatteryPercent = headsetBatteryPercent;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public boolean getVideoState() {
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

    public DataPacket clone() {
        DataPacket packet = new DataPacket();
        packet.setAlphaPct(this.alphaPct);
        packet.setAlphaPct(this.betaPct);
        packet.setHeadsetBatteryPercent(this.headsetBatteryPercent);
        packet.setMuseBatteryPercent(this.museBatteryPercent);
        packet.setMuseSensorsState(this.museSensorsState.clone());
        //packet.setTimestamp(this.timestamp);
        packet.setMuseState(this.museState);
        packet.setVideoName(this.videoName);
        packet.setVideoState(this.videoState);
        return packet;
    }

    @Override
    public PacketType type() {
        return PacketType.DATA;
    }
}
