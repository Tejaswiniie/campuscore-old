package com.campuscore;

/** Attendance colour rule: below 60 red, 60 to below 75 orange, 75 and above green. */
public final class AttendanceZone {
    private AttendanceZone() {}

    public static String of(double attendance) {
        if (attendance >= 75) return "green";
        if (attendance >= 60) return "orange";
        return "red";
    }
}
