package ru.spbstu.videomood.database;

import android.provider.BaseColumns;

public final class VideoMoodDataContract {
    private VideoMoodDataContract() {}

    public static abstract class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "user";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_FIRSTNAME = "firstname";
        public static final String COLUMN_NAME_LASTNAME = "lastname";
        public static final String COLUMN_NAME_BIRTHDATE = "birthdate";
        public static final String COLUMN_NAME_SEX = "sex";
    }

    public static abstract class SessionEntry implements BaseColumns {
        public static final String TABLE_NAME = "session";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_USER_ID = "user_id";
        public static final String COLUMN_NAME_DATESTART = "datestart";
        public static final String COLUMN_NAME_DATEFINISH = "datefinish";
        public static final String COLUMN_NAME_DATA = "data";
    }
}
