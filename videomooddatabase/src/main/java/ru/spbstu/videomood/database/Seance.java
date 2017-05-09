package ru.spbstu.videomood.database;

import com.google.gson.Gson;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@DatabaseTable(tableName = "seances")
public class Seance {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    public User user;

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd'T'HH:mm:ss");

    @DatabaseField
    private String dateFrom;

    @DatabaseField
    private String dateTo;

    @DatabaseField
    private String comment;

    @DatabaseField
    private String action;

    @ForeignCollectionField
    private ForeignCollection<SeanceVideo> seanceVideos;

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

    public ForeignCollection<SeanceVideo> getSeanceVideos() {
        return seanceVideos;
    }

    public void setSeanceVideos(ForeignCollection<SeanceVideo> seanceVideos) {
        this.seanceVideos = seanceVideos;
    }
}
