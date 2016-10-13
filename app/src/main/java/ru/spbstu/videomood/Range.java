package ru.spbstu.videomood;

public final class Range<T extends Comparable<? super T>> {

    private T mLower;
    private T mUpper;

    public Range(final T lower, final T upper) {
        mLower = lower;
        mUpper = upper;

        if (lower.compareTo(upper) > 0) {
            throw new IllegalArgumentException("lower must be less than or equal to upper");
        }
    }

    public boolean contains(T value) {
        boolean gteLower = value.compareTo(mLower) >= 0;
        boolean lteUpper  = value.compareTo(mUpper) <= 0;

        return gteLower && lteUpper;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (obj instanceof Range) {
            Range other = (Range) obj;
            return mLower.equals(other.mLower) && mUpper.equals(other.mUpper);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s - %s", mLower.toString(), mUpper.toString());
    }
}
