package com.campuscore;

/** Attendance colour rule: below 60 red, 60 to below 75 orange, 75 and above green. */
public final class AttendanceZone {
    private AttendanceZone() {}

    public static String of(double attendance) {
        if (attendance >= 75) return "green";
        if (attendance >= 60) return "orange";
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
