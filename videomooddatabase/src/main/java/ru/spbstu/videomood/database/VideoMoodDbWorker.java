package ru.spbstu.videomood.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class VideoMoodDbWorker {

    private final VideoMoodDbHelper helper;

    public VideoMoodDbWorker(Context context) {
        helper = new VideoMoodDbHelper(context);
    }

    public User createUser(@NonNull User user) {
        // Gets the data repository in write mode
        SQLiteDatabase db = helper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(VideoMoodDataContract.UserEntry.COLUMN_NAME_FIRSTNAME, user.firstName);
        values.put(VideoMoodDataContract.UserEntry.COLUMN_NAME_LASTNAME, user.lastName);
        values.put(VideoMoodDataContract.UserEntry.COLUMN_NAME_SEX, user.sex);
        values.put(VideoMoodDataContract.UserEntry.COLUMN_NAME_BIRTHDATE, user.birthDateStr);

        // Insert the new row, returning the primary key value of the new row
        long newUserId = db.insert(VideoMoodDataContract.UserEntry.TABLE_NAME, null, values);
        db.close();
        user.id = (int) newUserId;
        return user;
    }

    private final String[] projection = {
            VideoMoodDataContract.UserEntry.COLUMN_NAME_ID,
            VideoMoodDataContract.UserEntry.COLUMN_NAME_FIRSTNAME,
            VideoMoodDataContract.UserEntry.COLUMN_NAME_LASTNAME,
            VideoMoodDataContract.UserEntry.COLUMN_NAME_BIRTHDATE,
            VideoMoodDataContract.UserEntry.COLUMN_NAME_SEX
    };

    public User getUser(int id){
        SQLiteDatabase db = helper.getWritableDatabase();

        String sortOrder = VideoMoodDataContract.UserEntry.COLUMN_NAME_ID;

        String[] selectionArgs = {Integer.toString(id)};
        String selection = VideoMoodDataContract.UserEntry.COLUMN_NAME_ID + "=?";
        Cursor cursor = db.query(
            VideoMoodDataContract.UserEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        );
        cursor.moveToFirst();

        User user = createFrom(cursor);
        cursor.close();
        db.close();
        return user;
    }

    @NonNull
    private User createFrom(Cursor cursor) {
        User user = new User();
        user.id = cursor.getInt(cursor.getColumnIndexOrThrow(VideoMoodDataContract.UserEntry.COLUMN_NAME_ID));
        user.firstName = cursor.getString(cursor.getColumnIndexOrThrow(VideoMoodDataContract.UserEntry.COLUMN_NAME_FIRSTNAME));
        user.lastName = cursor.getString(cursor.getColumnIndexOrThrow(VideoMoodDataContract.UserEntry.COLUMN_NAME_LASTNAME));
        user.birthDateStr = cursor.getString(cursor.getColumnIndexOrThrow(VideoMoodDataContract.UserEntry.COLUMN_NAME_BIRTHDATE));
        user.sex = cursor.getString(cursor.getColumnIndexOrThrow(VideoMoodDataContract.UserEntry.COLUMN_NAME_SEX));
        return user;
    }

    public List<User> getUsers(){
        SQLiteDatabase db = helper.getWritableDatabase();

        Cursor cursor = db.query(
            VideoMoodDataContract.UserEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            null
        );

        ArrayList<User> arrayList = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            User user = createFrom(cursor);
            arrayList.add(user);
        }

        cursor.close();
        db.close();

        return arrayList;
    }

    public void removeUser(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();

        String[] whereArgs = {Integer.toString(id)};
        db.delete(VideoMoodDataContract.UserEntry.TABLE_NAME, VideoMoodDataContract.UserEntry.COLUMN_NAME_ID + "=?", whereArgs);

        db.close();
    }

    public Seance createSeance(){
        return null;
    }

    public Seance getSeance(int id){
        return null;
    }

    public List<Seance> getSeances(int userId){
        return new ArrayList<>();
    }

    public void removeSeance(int seanceId) {

    }
}
