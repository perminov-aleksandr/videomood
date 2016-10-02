package ru.spbstu.videomood;

public class AgeRange {
    private int from;
    private int to;

    public AgeRange(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public int getFrom(){
        return from;
    }

    public int getTo() {
        return to;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(from);
        sb.append("-");
        sb.append(to);
        return sb.toString();
    }
}
