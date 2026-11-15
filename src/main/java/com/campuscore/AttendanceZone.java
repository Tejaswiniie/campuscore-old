package com.campuscore;

/** Attendance colour rule: 75 and above green, 60 to below 75 orange, below 60 red. */
public final class AttendanceZone {
    private AttendanceZone() {}

    public static String of(double percent) {
        if (percent >= 75) return "green";
        if (percent >= 60) return "orange";
        return "red";
    }
}
