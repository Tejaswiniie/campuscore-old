package com.campuscore;

/** CGPA colour rule (out of 10): 8 and above green, 6 to below 8 orange, below 6 red. */
public final class CgpaZone {
    private CgpaZone() {}

    public static String of(double cgpa) {
        if (cgpa >= 8) return "green";
        if (cgpa >= 6) return "orange";
        return "red";
    }
}
