package ru.spbstu.videomood.btservice;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class VideoActivityState extends Packet {

    public VideoActivityState() {}

    public static VideoActivityState createFrom(String json) {
        return gson.fromJson(json, VideoActivityState.class);
    }

    @Nullable
    public static VideoActivityState createFrom(byte[] bytes) {
        try {
            return createFrom(new String(bytes, Constants.DEFAULT_CHARSET));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    //private long timestamp;

    private MuseState museState;

    private Integer museBatteryPercent;

    private Boolean[] museSensorsState;

    private Integer alphaPct;

    private Integer betaPct;

    private boolean isPanic;

    private Integer headsetBatteryPercent;

    private String videoName;

    private Boolean isVideoPlaying;

    private Integer currentPosition;

    private Integer duration;

    private ArrayList<VideoItem> videoList;

    public MuseState getMuseState() {
        return museState;
    }

    public void setMuseState(MuseState museState) {
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

    public void setMuseSensorsState(boolean[] museSensorsState) {
        Boolean[] sensorStates = new Boolean[museSensorsState.length];
        for (int i = 0; i < sensorStates.length; i++) {
            sensorStates[i] = museSensorsState[i];
        }
        setMuseSensorsState(sensorStates);
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

    public Boolean getIsVideoPlaying() {
        return isVideoPlaying;
    }

    public void setIsVideoPlaying(boolean isPlaying) {
        this.isVideoPlaying = isPlaying;
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

    public boolean isPanic() {
        return isPanic;
    }

    public void setIsPanic(boolean panic) {
        isPanic = panic;
    }

    private static final Gson gson = new GsonBuilder().setLenient().create();

    private String toJson() {
        return gson.toJson(this, VideoActivityState.class);
    }

    @Override
    public byte[] toBytes() {
        try {
            String json = this.toJson();
            return json.getBytes(Constants.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer getCurrentPositionSec() {
        return currentPosition;
    }

    public void setCurrentPositionSec(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public Integer getDurationSec() {
        return duration;
    }

    public void setDurationSec(int duration) {
        this.duration = duration;
    }
}
