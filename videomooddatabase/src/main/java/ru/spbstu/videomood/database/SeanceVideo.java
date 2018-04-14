package ru.spbstu.videomood.database;

import com.google.gson.Gson;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@DatabaseTable(tableName = "seancevideos")
public class SeanceVideo {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    public Video video;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    public Seance seance;

    @DatabaseField(canBeNull = false)
    private long timestamp;

    @DatabaseField
    private String dataStr;

    public String getDataStr() {
        return dataStr;
    }

    private List<SeanceDataEntry> data;

    public List<SeanceDataEntry> getData() {
        if (data == null || data.isEmpty() && !dataStr.equals("") && dataStr!= null)
            setData(dataStr);

        return data;
    }

    public void setData(List<SeanceDataEntry> data) {
        this.data = data;

        this.dataStr = new Gson().toJson(data.toArray(), SeanceDataEntry[].class);
    }

    public void setData(String dataStr) {
        this.dataStr = dataStr;

        SeanceDataEntry[] data = new Gson().fromJson(dataStr, SeanceDataEntry[].class);
        this.data = new ArrayList<>();
        Collections.addAll(this.data, data);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        if (timestamp == 0)
            timestamp = getData().size();

        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
