package com.campuscore;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** The CGPA and attendance rings: values, labels, colours, progress and what is (not) printed. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RingTest {

    @Autowired MockMvc mvc;
    @Autowired AdminService admin;
    @Autowired Fixtures fx;

    // ---------- progress maths (no Spring needed for these, but they live with the other ring tests) ----------

    @Test void progressIsProportionalToTheStoredValue() {
        assertEquals(82.5, Gauge.percent(82.5, 100), 0.0001);
        assertEquals(84.5, Gauge.percent(8.45, 10), 0.0001);
        assertEquals(50.0, Gauge.percent(5.0, 10), 0.0001);
        assertEquals(0.0, Gauge.percent(0, 10), 0.0001);
        assertEquals(100.0, Gauge.percent(10, 10), 0.0001);
    }

    @Test void progressNeverExceedsTheRingAndInvalidValuesGiveAnEmptyRing() {
        assertEquals(100.0, Gauge.percent(120, 100), 0.0001);
        assertEquals(100.0, Gauge.percent(10.5, 10), 0.0001);
        assertEquals(0.0, Gauge.percent(-3, 100), 0.0001);
        assertEquals(0.0, Gauge.percent(Double.NaN, 10), 0.0001);
        assertEquals(0.0, Gauge.percent(Double.POSITIVE_INFINITY, 10), 0.0001);
        assertEquals(0.0, Gauge.percent(5, 0), 0.0001);
    }

    // ---------- classification ----------

    @Test void cgpaLabelsAndColoursFollowTheThresholds() {
        assertEquals("Excellent", CgpaZone.label(10.0));
        assertEquals("Excellent", CgpaZone.label(8.0));
        assertEquals("Satisfactory", CgpaZone.label(7.99));
        assertEquals("Satisfactory", CgpaZone.label(6.0));
        assertEquals("Needs Improvement", CgpaZone.label(5.99));
        assertEquals("Needs Improvement", CgpaZone.label(0));
        assertEquals("green", CgpaZone.of(8.0));
        assertEquals("orange", CgpaZone.of(7.99));
        assertEquals("orange", CgpaZone.of(6.0));
        assertEquals("red", CgpaZone.of(5.99));
    }

    @Test void attendanceLabelsAndColoursFollowTheThresholds() {
        assertEquals("Excellent", AttendanceZone.label(100));
        assertEquals("Excellent", AttendanceZone.label(75));
        assertEquals("Satisfactory", AttendanceZone.label(74.99));
        assertEquals("Satisfactory", AttendanceZone.label(60));
        assertEquals("Needs Improvement", AttendanceZone.label(59.99));
        assertEquals("green", AttendanceZone.of(75));
        assertEquals("orange", AttendanceZone.of(74.99));
        assertEquals("orange", AttendanceZone.of(60));
        assertEquals("red", AttendanceZone.of(59.99));
    }

    // ---------- invalid values never get stored ----------

    @Test void attendanceAbove100AndCgpaAbove10AreRejected() {
        Section sec = fx.section("RG0", 1, "A");
        assertThrows(IllegalArgumentException.class, () -> admin.createStudent("RG0S1", "X", sec.getId(), 100.01, 7.0));
        assertThrows(IllegalArgumentException.class, () -> admin.createStudent("RG0S2", "X", sec.getId(), 80.0, 10.01));
        assertThrows(IllegalArgumentException.class, () -> admin.createStudent("RG0S3", "X", sec.getId(), -1.0, 7.0));
        assertThrows(IllegalArgumentException.class, () -> admin.createStudent("RG0S4", "X", sec.getId(), 80.0, -0.5));
    }

    // ---------- the dashboard ----------

    @Test @WithMockUser(username = "RG1S1", roles = "STUDENT") void excellentValuesShowGreenRingsWithTheExactProgress() throws Exception {
        admin.createStudent("RG1S1", "Student RG1", fx.section("RG1", 2, "A").getId(), 82.5, 8.45);
        mvc.perform(get("/student")).andExpect(status().isOk())
            .andExpect(content().string(containsString(">82.5%<")))
            .andExpect(content().string(containsString(">8.45<")))
            .andExpect(content().string(containsString("stroke-dasharray=\"82.5 100\"")))
            .andExpect(content().string(containsString("stroke-dasharray=\"84.5 100\"")))
            .andExpect(content().string(containsString("ring green")))
            .andExpect(content().string(containsString("Excellent")));
    }

    @Test @WithMockUser(username = "RG2S1", roles = "STUDENT") void satisfactoryValuesShowAmberRings() throws Exception {
        admin.createStudent("RG2S1", "Student RG2", fx.section("RG2", 2, "A").getId(), 65.0, 7.0);
        mvc.perform(get("/student")).andExpect(status().isOk())
            .andExpect(content().string(containsString("ring orange")))
            .andExpect(content().string(containsString("Satisfactory")));
    }

    @Test @WithMockUser(username = "RG3S1", roles = "STUDENT") void lowValuesShowRedRings() throws Exception {
        admin.createStudent("RG3S1", "Student RG3", fx.section("RG3", 2, "A").getId(), 55.0, 5.5);
        mvc.perform(get("/student")).andExpect(status().isOk())
            .andExpect(content().string(containsString("ring red")))
            .andExpect(content().string(containsString("Needs Improvement")));
    }

    @Test @WithMockUser(username = "RG4S1", roles = "STUDENT") void noThresholdExplanationsAreDisplayed() throws Exception {
        admin.createStudent("RG4S1", "Student RG4", fx.section("RG4", 2, "A").getId(), 82.5, 8.45);
        mvc.perform(get("/student")).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("and above is green"))))
            .andExpect(content().string(not(containsString("Out of 10"))))
            .andExpect(content().string(not(containsString("Good standing"))));
    }

    @Test @WithMockUser(username = "RG5NOREC", roles = "STUDENT") void aMissingStudentRecordShowsANeutralRingInsteadOfAFakeValue() throws Exception {
        mvc.perform(get("/student")).andExpect(status().isOk())
            .andExpect(content().string(containsString("ring none")))
            .andExpect(content().string(containsString("No data")))
            .andExpect(content().string(not(containsString("stroke-dasharray"))));
    }
}
