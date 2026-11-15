package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CgpaZoneTest {
    @Test void eightAndAboveIsGreen() {
        assertEquals("green", CgpaZone.of(8));
        assertEquals("green", CgpaZone.of(9.5));
    }
    @Test void sixToBelowEightIsOrange() {
        assertEquals("orange", CgpaZone.of(6));
        assertEquals("orange", CgpaZone.of(7.99));
    }
    @Test void belowSixIsRed() {
        assertEquals("red", CgpaZone.of(5.9));
    }
}
