package ru.spbstu.videomood.btservice;

import android.support.annotation.Nullable;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class DataPacket extends Packet {

    public DataPacket() {}

    public static DataPacket createFrom(String json) {
        return gson.fromJson(json, DataPacket.class);
    }

    @Nullable
    public static DataPacket createFrom(byte[] bytes) {
        try {
            return createFrom(new String(bytes, Constants.DEFAULT_CHARSET));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    //private long timestamp;

    private Boolean museState;

    private Integer museBatteryPercent;

    private Boolean[] museSensorsState;

    private Integer alphaPct;

    private Integer betaPct;

    private boolean isPanic;

    private Integer headsetBatteryPercent;

    private String videoName;

    private Boolean videoState;

    private Integer currentPosition;

    private Integer duration;

    private ArrayList<VideoItem> videoList;

    private String screenshot;

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

    public boolean isPanic() {
        return isPanic;
    }

    public void setIsPanic(boolean panic) {
        isPanic = panic;
    }

    public void setScreenshot(byte[] screenshot) {
        if (screenshot != null)
            this.screenshot = Base64.encodeToString(screenshot, Base64.DEFAULT);
    }

    public byte[] getScreenshot() {
        if (this.screenshot != null)
            return Base64.decode(this.screenshot, Base64.DEFAULT);
        else
            return null;
    }

    private static final Gson gson = new GsonBuilder().setLenient().create();

    private String toJson() {
        return gson.toJson(this, DataPacket.class);
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
