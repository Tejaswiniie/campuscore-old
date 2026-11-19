package com.campuscore;

/** Maths for the circular progress indicators. */
public final class Gauge {
    private Gauge() {}

    /**
     * How full a ring is, from 0 to 100, rounded to two decimals. A missing, negative or invalid value gives an
     * empty ring and a value at or above the maximum gives a full ring, so a ring can never show more than the truth.
     */
    public static double percent(double value, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value) || max <= 0 || value <= 0) return 0;
        if (value >= max) return 100;
        return Math.round(value / max * 10000.0) / 100.0;
    }
}
