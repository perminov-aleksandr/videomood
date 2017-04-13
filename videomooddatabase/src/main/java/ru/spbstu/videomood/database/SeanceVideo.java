package ru.spbstu.videomood.database;

public class SeanceVideo {
    private int id;

    private int videoId;

    private int seanceId;

    private int startTimeSec;

    private int endTimeSec;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVideoId() {
        return videoId;
    }

    public void setVideoId(int videoId) {
        this.videoId = videoId;
    }

    public int getSeanceId() {
        return seanceId;
    }

    public void setSeanceId(int seanceId) {
        this.seanceId = seanceId;
    }

    public int getStartTimeSec() {
        return startTimeSec;
    }

    public void setStartTimeSec(int startTimeSec) {
        this.startTimeSec = startTimeSec;
    }

    public int getEndTimeSec() {
        return endTimeSec;
    }

    public void setEndTimeSec(int endTimeSec) {
        this.endTimeSec = endTimeSec;
    }
}
