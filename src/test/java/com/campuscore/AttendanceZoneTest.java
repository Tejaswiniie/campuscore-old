package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AttendanceZoneTest {
    @Test void below60IsRed() {
        assertEquals("red", AttendanceZone.of(0));
        assertEquals("red", AttendanceZone.of(59.9));
    }

    @Test void from60ToBelow75IsOrange() {
        assertEquals("orange", AttendanceZone.of(60));
        assertEquals("orange", AttendanceZone.of(74.9));
    }

    @Test void from75IsGreen() {
        assertEquals("green", AttendanceZone.of(75));
        assertEquals("green", AttendanceZone.of(100));
    }
}
