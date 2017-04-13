package ru.spbstu.videomood.database;

import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Seance{
    private int id;

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd'T'HH:mm:ss");

    private String dateFrom;

    private String dateTo;

    private int userId;

    private String comment;

    private String action;

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

        this.dataStr = new Gson().toJson(data.toArray(), SeanceDataEntry[].class);
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
