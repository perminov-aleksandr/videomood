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

    public static void setRange(Range<Integer> range){
        ageRange = range;
    }

    public static Range<Integer> getAgeRange() {
        return ageRange;
    }
}
