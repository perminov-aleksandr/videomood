package ru.spbstu.videomood;

public final class User {
    private User() {
    }

    private static Mood currentMood;

    public static Mood getCurrentMood() {
        return currentMood;
    }

    public static void setCurrentMood(Mood mood) {
        currentMood = mood;
    }

    private static Range<Integer> ageRange;

    private static int ageRangeIndex;

    public static void setRange(Range<Integer> range){
        ageRangeIndex = -1;
        ageRange = range;
    }

    public static Range<Integer> getAgeRange() {
        return ageRange;
    }

    public static int getAgeRangeIndex() {
        if (ageRangeIndex == -1) {
            for (ageRangeIndex = 0; ageRangeIndex < Const.ageRanges.length; ageRangeIndex++)
                if (Const.ageRanges[ageRangeIndex] == ageRange)
                    break;
        }

        return ageRangeIndex;
    }
}
