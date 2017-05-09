package ru.spbstu.videomood.database;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "videos")
public class Video {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false, index = true)
    private String path;

    @DatabaseField(canBeNull = false)
    private int duration;

    @DatabaseField
    private boolean isAbsent;

    @ForeignCollectionField
    public ForeignCollection<VideoTag> tags;

    @ForeignCollectionField
    public ForeignCollection<VideoAgeCategory> ageCategories;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getDuration() {
        return duration;
    }

    public static String getDurationDisplayString(int duration) {
        int mins = duration / 60;
        int secs = duration % 60;
        return String.format("%d:%02d", mins, secs);
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
