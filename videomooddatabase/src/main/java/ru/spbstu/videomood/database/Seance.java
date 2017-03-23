package ru.spbstu.videomood.database;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Seance{
    private int id;

    private String dateFrom;

    private String dateTo;

    private int userId;

    private String dataStr;

    public String getDataStr() {
        return dataStr;
    }

    private List<SeanceDataEntry> data;

    public List<SeanceDataEntry> getData() {
        return data;
    }

    public void setData(List<SeanceDataEntry> data) {
        this.data = data;
    }

    public void setData(String dataStr) {
        this.dataStr = dataStr;

        SeanceDataEntry[] data = new Gson().fromJson(dataStr, SeanceDataEntry[].class);
        this.data = new ArrayList<>();
        Collections.addAll(this.data, data);
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
