package ru.spbstu.videomood;

public class MuseMoodSolver {

    private User user;

    private MuseManager museManager;

    public MuseMoodSolver(MuseManager museManager, User user) {
        this.user = user;
        this.museManager = museManager;
    }

    public Mood solve() {
        return user.getCurrentMood();
    }
}
