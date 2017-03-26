package ru.spbstu.videomood.database;

public enum Sex {
    MALE,
    FEMALE;

    private static final String femaleStr = "F";
    private static final String maleStr = "M";

    public static String toString(Sex sex) {
        switch (sex){
            case FEMALE:
                return femaleStr;
            case MALE:
                return maleStr;
        }
        return null;
    }

    public static Sex get(String sex) {
        if (sex.equals(femaleStr))
            return FEMALE;
        else
            return MALE;
    }
}
