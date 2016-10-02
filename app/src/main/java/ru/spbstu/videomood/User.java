package ru.spbstu.videomood;

public class User {
    public User(AgeRange ageRange) {
        this.ageRange = ageRange;
    }

    private Mood currentMood;

    public Mood getCurrentMood() {
        return currentMood;
    }

    private AgeRange ageRange;

    public AgeRange getAgeRange() {
        return ageRange;
    }
}
