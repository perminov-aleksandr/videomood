package ru.spbstu.videomood.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "seancevideos")
public class SeanceVideo {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    public Video video;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    public Seance seance;

    @DatabaseField(canBeNull = false)
    private int startTimeSec;

    @DatabaseField(canBeNull = false)
    private int endTimeSec;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
