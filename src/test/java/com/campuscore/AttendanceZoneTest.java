package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AttendanceZoneTest {
    @Test void seventyFiveAndAboveIsGreen() {
        assertEquals("green", AttendanceZone.of(75));
        assertEquals("green", AttendanceZone.of(90));
    }
    @Test void sixtyToBelowSeventyFiveIsOrange() {
        assertEquals("orange", AttendanceZone.of(60));
        assertEquals("orange", AttendanceZone.of(68));
    }
    @Test void belowSixtyIsRed() {
        assertEquals("red", AttendanceZone.of(59.9));
        assertEquals("red", AttendanceZone.of(0));
    }
}
