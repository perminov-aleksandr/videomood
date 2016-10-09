package ru.spbstu.videomood;

import android.util.Range;

public class User {
    public User(Range<Integer> ageRange) {
        this.ageRange = ageRange;
    }

    private Mood currentMood;

    public Mood getCurrentMood() {
        return currentMood;
    }

    public void setCurrentMood(Mood mood) {
        currentMood = mood;
    }

    private Range<Integer> ageRange;

    public Range<Integer> getAgeRange() {
        return ageRange;
    }
}
