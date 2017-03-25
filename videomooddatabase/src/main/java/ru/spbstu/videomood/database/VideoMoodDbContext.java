package ru.spbstu.videomood.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class VideoMoodDbContext {

    private final VideoMoodDbHelper helper;

    public VideoMoodDbContext(Context context) {
        helper = new VideoMoodDbHelper(context);
    }

    public User createUser(@NonNull User user) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(VideoMoodDataContract.UserEntry.COLUMN_NAME_FIRSTNAME, user.firstName);
        values.put(VideoMoodDataContract.UserEntry.COLUMN_NAME_LASTNAME, user.lastName);
        values.put(VideoMoodDataContract.UserEntry.COLUMN_NAME_SEX, user.sex);
        values.put(VideoMoodDataContract.UserEntry.COLUMN_NAME_BIRTHDATE, user.birthDateStr);

        long newUserId = db.insert(VideoMoodDataContract.UserEntry.TABLE_NAME, null, values);
        user.id = (int) newUserId;

        db.close();
        return user;
    }

    private final String[] userProjection = {
            VideoMoodDataContract.UserEntry.COLUMN_NAME_ID,
            VideoMoodDataContract.UserEntry.COLUMN_NAME_FIRSTNAME,
            VideoMoodDataContract.UserEntry.COLUMN_NAME_LASTNAME,
            VideoMoodDataContract.UserEntry.COLUMN_NAME_BIRTHDATE,
            VideoMoodDataContract.UserEntry.COLUMN_NAME_SEX
    };

    private final String[] seanceProjection = {
            VideoMoodDataContract.SessionEntry.COLUMN_NAME_ID,
            VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATESTART,
            VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATEFINISH,
            VideoMoodDataContract.SessionEntry.COLUMN_NAME_USER_ID,
            VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATA
    };

    public User getUser(int id){
        SQLiteDatabase db = helper.getWritableDatabase();

        String sortOrder = VideoMoodDataContract.UserEntry.COLUMN_NAME_ID;

        String[] selectionArgs = {Integer.toString(id)};
        String selection = VideoMoodDataContract.UserEntry.COLUMN_NAME_ID + "=?";
        Cursor cursor = db.query(
            VideoMoodDataContract.UserEntry.TABLE_NAME,
                userProjection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        );
        cursor.moveToFirst();

        User user = createUserFrom(cursor);
        cursor.close();
        db.close();
        return user;
    }

    @NonNull
    private User createUserFrom(Cursor cursor) {
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
                userProjection,
            null,
            null,
            null,
            null,
            null
        );

        ArrayList<User> arrayList = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            User user = createUserFrom(cursor);
            arrayList.add(user);
        }

        cursor.close();
        db.close();

        return arrayList;
    }

    private Seance createSeanceFrom(Cursor cursor) {
        Seance seance = new Seance();
        seance.setId(cursor.getColumnIndexOrThrow(VideoMoodDataContract.SessionEntry.COLUMN_NAME_ID));
        seance.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(VideoMoodDataContract.SessionEntry.COLUMN_NAME_USER_ID)));
        seance.setDateFrom(cursor.getString(cursor.getColumnIndexOrThrow(VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATESTART)));
        seance.setDateTo(cursor.getString(cursor.getColumnIndexOrThrow(VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATEFINISH)));
        seance.setData(cursor.getString(cursor.getColumnIndexOrThrow(VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATA)));
        return seance;
    }

    public void removeUser(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();

        String[] whereArgs = {Integer.toString(id)};
        db.delete(VideoMoodDataContract.UserEntry.TABLE_NAME, VideoMoodDataContract.UserEntry.COLUMN_NAME_ID + "=?", whereArgs);

        db.close();
    }

    public Seance createSeance(@NonNull Seance seance) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATESTART, seance.getDateFrom());
        values.put(VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATEFINISH, seance.getDateTo());
        values.put(VideoMoodDataContract.SessionEntry.COLUMN_NAME_USER_ID, seance.getUserId());
        values.put(VideoMoodDataContract.SessionEntry.COLUMN_NAME_DATA, seance.getDataStr());

        long newSeanceId = db.insert(VideoMoodDataContract.SessionEntry.TABLE_NAME, null, values);
        seance.setId((int) newSeanceId);

        db.close();
        return seance;
    }

    public Seance getSeance(int id){
        SQLiteDatabase db = helper.getWritableDatabase();

        String sortOrder = VideoMoodDataContract.SessionEntry.COLUMN_NAME_ID;

        String[] selectionArgs = {Integer.toString(id)};
        String selection = VideoMoodDataContract.SessionEntry.COLUMN_NAME_ID + "=?";
        Cursor cursor = db.query(
                VideoMoodDataContract.SessionEntry.TABLE_NAME,
                seanceProjection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
        cursor.moveToFirst();

        Seance seance = createSeanceFrom(cursor);
        cursor.close();
        db.close();
        return seance;
    }

    public List<Seance> getSeances(int userId){
        SQLiteDatabase db = helper.getWritableDatabase();

        Cursor cursor = db.query(
            VideoMoodDataContract.SessionEntry.TABLE_NAME,
            seanceProjection,
            VideoMoodDataContract.SessionEntry.COLUMN_NAME_USER_ID + "=?",
            new String[]{ Integer.toString(userId) },
            null,
            null,
            null
        );

        ArrayList<Seance> arrayList = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            Seance seance = createSeanceFrom(cursor);
            arrayList.add(seance);
        }

        cursor.close();
        db.close();

        return arrayList;
    }

    public void removeSeance(int seanceId) {
        SQLiteDatabase db = helper.getWritableDatabase();

        String[] whereArgs = {Integer.toString(seanceId)};
        db.delete(VideoMoodDataContract.SessionEntry.TABLE_NAME, VideoMoodDataContract.SessionEntry.COLUMN_NAME_ID + "=?", whereArgs);

        db.close();
    }
}
