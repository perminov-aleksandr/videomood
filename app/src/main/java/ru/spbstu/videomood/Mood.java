package ru.spbstu.videomood;

public enum Mood {
    AWFUL("Awful"),
    BAD("Bad"),
    NORMAL("Normal"),
    GOOD("Good"),
    GREAT("Great");

    private final String value;

    private Mood(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }
}
