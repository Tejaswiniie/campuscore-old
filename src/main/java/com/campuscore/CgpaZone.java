package com.campuscore;

/** CGPA colour rule (out of 10): 8 and above green, 6 to below 8 orange, below 6 red. */
public final class CgpaZone {
    private CgpaZone() {}

    public static String of(double cgpa) {
        if (cgpa >= 8) return "green";
        if (cgpa >= 6) return "orange";
        return "red";
    }

    /** The status word shown with the ring. Always one of Excellent, Satisfactory or Needs Improvement. */
    public static String label(double value) {
        String zone = of(value);
        if (zone.equals("green")) return "Excellent";
        if (zone.equals("orange")) return "Satisfactory";
        return "Needs Improvement";
    }
}
