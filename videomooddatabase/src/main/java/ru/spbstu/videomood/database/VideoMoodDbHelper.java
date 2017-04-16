package ru.spbstu.videomood.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class VideoMoodDbHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "videomood.db";

    private static final int DATABASE_VERSION = 1;

    public VideoMoodDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final Class[] entityClasses = new Class[]{
        AgeCategory.class,
        Tag.class,
        User.class,
        Video.class,
        VideoTag.class,
        VideoAgeCategory.class,
        Seance.class,
        SeanceVideo.class
    };

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            Log.i(VideoMoodDbHelper.class.getName(), "onCreate");
            for (int i = 0; i < entityClasses.length; i++) {
                Class entityClass = entityClasses[i];
                TableUtils.createTable(connectionSource, entityClass);
            }
        } catch (SQLException e) {
            Log.e(VideoMoodDbHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(VideoMoodDbHelper.class.getName(), "onUpgrade");
            for (int i = 0; i < entityClasses.length; i++) {
                TableUtils.dropTable(connectionSource, entityClasses[i], true);
            }
            // after we drop the old databases, we create the new ones
            onCreate(database, connectionSource);
        } catch (SQLException e) {
            Log.e(VideoMoodDbHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    private Dao<User, Integer> userDao = null;

    /**
     * Returns the Database Access Object (DAO) for our SimpleData class. It will create it or just give the cached
     * value.
     */
    public Dao<User, Integer> getUserDao() throws SQLException {
        if (userDao == null) {
            userDao = getDao(User.class);
        }
        return userDao;
    }
}