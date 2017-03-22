package ru.spbstu.videomood.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class VideoMoodDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "VideoMood.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_USERS =
            "CREATE TABLE " + VideoMoodDataContract.UserEntry.TABLE_NAME + " (" +
                    VideoMoodDataContract.UserEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    VideoMoodDataContract.UserEntry.COLUMN_NAME_FIRSTNAME + TEXT_TYPE + COMMA_SEP +
                    VideoMoodDataContract.UserEntry.COLUMN_NAME_LASTNAME + TEXT_TYPE + COMMA_SEP +
                    VideoMoodDataContract.UserEntry.COLUMN_NAME_BIRTHDATE + TEXT_TYPE + COMMA_SEP +
                    VideoMoodDataContract.UserEntry.COLUMN_NAME_SEX + TEXT_TYPE +
            " )";

    private static final String SQL_CREATE_SEANCES =
            "CREATE TABLE " + VideoMoodDataContract.SessionEntry.TABLE_NAME + " (" +
                    VideoMoodDataContract.SessionEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    VideoMoodDataContract.SessionEntry.COLUMN_NAME_USER_ID + TEXT_TYPE + COMMA_SEP +
                    VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATESTART + TEXT_TYPE + COMMA_SEP +
                    VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATEFINISH + TEXT_TYPE + COMMA_SEP +
                    VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATA + TEXT_TYPE + COMMA_SEP +
                    "FOREIGN KEY(" + VideoMoodDataContract.SessionEntry.COLUMN_NAME_USER_ID + ") REFERENCES "
                    + VideoMoodDataContract.UserEntry.TABLE_NAME + "(" + VideoMoodDataContract.UserEntry.COLUMN_NAME_ID + ")" +
                    " )";

    private static final String SQL_DELETE_USERS =
            "DROP TABLE IF EXISTS " + VideoMoodDataContract.UserEntry.TABLE_NAME;
    private static final String SQL_DELETE_SEANCES =
            "DROP TABLE IF EXISTS " + VideoMoodDataContract.SessionEntry.TABLE_NAME;


    public VideoMoodDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_SEANCES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_USERS);
        db.execSQL(SQL_DELETE_SEANCES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}