package com.campuscore;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Department -> academic year -> section. The admin registers sections; the student form only offers those.
 * The server checks the combination again, so a hand-made request cannot get around the drop-downs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SectionFilterTest {

    @Autowired AcademicService academic;
    @Autowired AdminService admin;
    @Autowired StudentRepository students;
    @Autowired Fixtures fx;
    @Autowired MockMvc mvc;

    private static List<String> names(List<Section> list) {
        List<String> out = new ArrayList<>();
        for (Section s : list) out.add(s.getName());
        return out;
    }

    /** CSE 1st year A, B; CSE 2nd year A; ECE 1st year A, as in the example of the requirements. */
    private Department[] structure(String cse, String ece) {
        fx.section(cse, 1, "A");
        fx.section(cse, 1, "B");
        fx.section(cse, 2, "A");
        fx.section(ece, 1, "A");
        return new Department[]{fx.dept(cse), fx.dept(ece)};
    }

    // ---------- the lists ----------

    @Test void sectionsAreFilteredByDepartmentAndYear() {
        Department[] d = structure("SF1C", "SF1E");
        assertEquals(List.of("A", "B"), names(academic.sectionsFor(d[0].getId(), 1)));
        assertEquals(List.of("A"), names(academic.sectionsFor(d[0].getId(), 2)));
        assertEquals(List.of("A"), names(academic.sectionsFor(d[1].getId(), 1)));
    }

    @Test void sectionsOfOtherDepartmentsAndYearsAreNeverIncluded() {
        Department[] d = structure("SF2C", "SF2E");
        for (Section s : academic.sectionsFor(d[0].getId(), 1)) {
            assertEquals(d[0].getId(), s.getDepartment().getId());
            assertEquals(1, s.getYearOfStudy());
        }
        assertEquals(1, academic.sectionsFor(d[1].getId(), 1).size());   // ECE has only its own section A
    }

    @Test void aYearWithoutSectionsGivesAnEmptyList() {
        Department[] d = structure("SF3C", "SF3E");
        assertTrue(academic.sectionsFor(d[1].getId(), 2).isEmpty());
        assertTrue(academic.sectionsFor(d[0].getId(), 4).isEmpty());
    }

    @Test void invalidDepartmentOrYearGivesAnEmptyListInsteadOfAnError() {
        Department[] d = structure("SF4C", "SF4E");
        assertTrue(academic.sectionsFor(null, 1).isEmpty());
        assertTrue(academic.sectionsFor(d[0].getId(), null).isEmpty());
        assertTrue(academic.sectionsFor(d[0].getId(), 0).isEmpty());
        assertTrue(academic.sectionsFor(d[0].getId(), 5).isEmpty());
        assertTrue(academic.sectionsFor(987654321L, 1).isEmpty());
    }

    @Test void aNewlyRegisteredSectionAppearsWithoutAnyCodeChange() {
        Department[] d = structure("SF5C", "SF5E");
        assertEquals(List.of("A", "B"), names(academic.sectionsFor(d[0].getId(), 1)));
        fx.section("SF5C", 1, "C");
        assertEquals(List.of("A", "B", "C"), names(academic.sectionsFor(d[0].getId(), 1)));
    }

    // ---------- server-side validation ----------

    @Test void aMatchingCombinationCreatesTheStudent() {
        Department[] d = structure("SF6C", "SF6E");
        Section a = academic.sectionsFor(d[0].getId(), 2).get(0);
        Student s = admin.createStudent("SF6S1", "Student", null, d[0].getId(), 2, a.getId(), 80.0, 7.5);
        assertEquals(a.getId(), s.getSection().getId());
    }

    @Test void mismatchedDepartmentYearOrSectionIsRejected() {
        Department[] d = structure("SF7C", "SF7E");
        Section cseYear1 = academic.sectionsFor(d[0].getId(), 1).get(0);
        // the section belongs to CSE year 1, so the department, the year or both being different must fail
        assertThrows(IllegalArgumentException.class,
            () -> admin.createStudent("SF7S1", "X", null, d[1].getId(), 1, cseYear1.getId(), 80.0, 7.5));
        assertThrows(IllegalArgumentException.class,
            () -> admin.createStudent("SF7S2", "X", null, d[0].getId(), 2, cseYear1.getId(), 80.0, 7.5));
        assertThrows(IllegalArgumentException.class,
            () -> admin.createStudent("SF7S3", "X", null, d[1].getId(), 3, cseYear1.getId(), 80.0, 7.5));
        assertFalse(students.existsById("SF7S1"));
        assertFalse(students.existsById("SF7S2"));
        assertFalse(students.existsById("SF7S3"));
    }

    @Test void missingOrOutOfRangeDepartmentAndYearAreRejected() {
        Department[] d = structure("SF8C", "SF8E");
        Section s = academic.sectionsFor(d[0].getId(), 1).get(0);
        assertThrows(IllegalArgumentException.class, () -> admin.createStudent("SF8S1", "X", null, null, 1, s.getId(), 80.0, 7.5));
        assertThrows(IllegalArgumentException.class, () -> admin.createStudent("SF8S2", "X", null, d[0].getId(), null, s.getId(), 80.0, 7.5));
        assertThrows(IllegalArgumentException.class, () -> admin.createStudent("SF8S3", "X", null, d[0].getId(), 7, s.getId(), 80.0, 7.5));
        assertThrows(IllegalArgumentException.class, () -> admin.createStudent("SF8S4", "X", null, d[0].getId(), 1, 987654321L, 80.0, 7.5));
    }

    @Test void editingKeepsAValidSectionAndRefusesAMismatch() {
        Department[] d = structure("SF9C", "SF9E");
        Section year1 = academic.sectionsFor(d[0].getId(), 1).get(0);
        Section year2 = academic.sectionsFor(d[0].getId(), 2).get(0);
        admin.createStudent("SF9S1", "Original", null, d[0].getId(), 1, year1.getId(), 80.0, 7.5);

        admin.updateStudent("SF9S1", "Renamed", null, d[0].getId(), 1, year1.getId(), 81.0, 7.6);     // same section, still valid
        assertEquals("Renamed", students.findById("SF9S1").orElseThrow().getName());
        assertEquals(year1.getId(), students.findById("SF9S1").orElseThrow().getSection().getId());

        admin.updateStudent("SF9S1", "Renamed", null, d[0].getId(), 2, year2.getId(), 81.0, 7.6);     // a valid move to year 2
        assertEquals(year2.getId(), students.findById("SF9S1").orElseThrow().getSection().getId());

        assertThrows(IllegalArgumentException.class,                                                // a mismatch changes nothing
            () -> admin.updateStudent("SF9S1", "Hacked", null, d[1].getId(), 2, year2.getId(), 81.0, 7.6));
        assertEquals("Renamed", students.findById("SF9S1").orElseThrow().getName());
    }

    @Test void theOlderServiceCallsStillWork() {
        Department[] d = structure("SF10C", "SF10E");
        Section s = academic.sectionsFor(d[0].getId(), 1).get(0);
        Student created = admin.createStudent("SF10S1", "Old style", s.getId(), 80.0, 7.5);
        assertEquals(s.getId(), created.getSection().getId());
    }

    // ---------- the endpoint ----------

    @Test @WithMockUser(roles = "ADMIN") void theEndpointReturnsOnlyTheMatchingSections() throws Exception {
        Department[] d = structure("SF11C", "SF11E");
        mvc.perform(get("/admin/section-options").param("departmentId", String.valueOf(d[0].getId())).param("year", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("A"))
            .andExpect(jsonPath("$[1].name").value("B"));
        mvc.perform(get("/admin/section-options").param("departmentId", String.valueOf(d[1].getId())).param("year", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test @WithMockUser(roles = "STUDENT") void studentsCannotUseTheEndpoint() throws Exception {
        mvc.perform(get("/admin/section-options").param("departmentId", "1").param("year", "1"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "FACULTY") void facultyCannotUseTheEndpoint() throws Exception {
        mvc.perform(get("/admin/section-options").param("departmentId", "1").param("year", "1"))
            .andExpect(status().isForbidden());
    }

    @Test void anonymousUsersAreSentToTheAdminLogin() throws Exception {
        mvc.perform(get("/admin/section-options").param("departmentId", "1").param("year", "1"))
            .andExpect(status().is3xxRedirection());
    }

    // ---------- the forms ----------

    @Test @WithMockUser(roles = "ADMIN") void aTamperedFormIsRefusedWithAClearMessage() throws Exception {
        Department[] d = structure("SF12C", "SF12E");
        Section cseYear1 = academic.sectionsFor(d[0].getId(), 1).get(0);
        mvc.perform(post("/admin/students").param("rollNo", "SF12S1").param("name", "Tampered")
                .param("departmentId", String.valueOf(d[1].getId())).param("yearOfStudy", "1")
                .param("sectionId", String.valueOf(cseYear1.getId())).param("attendance", "80").param("cgpa", "7").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("does not belong")));
        assertFalse(students.existsById("SF12S1"));
    }

    @Test @WithMockUser(roles = "ADMIN") void theFormCreatesAStudentForAMatchingChoice() throws Exception {
        Department[] d = structure("SF13C", "SF13E");
        Section s = academic.sectionsFor(d[0].getId(), 2).get(0);
        mvc.perform(post("/admin/students").param("rollNo", "SF13S1").param("name", "Good Choice")
                .param("departmentId", String.valueOf(d[0].getId())).param("yearOfStudy", "2")
                .param("sectionId", String.valueOf(s.getId())).param("attendance", "80").param("cgpa", "7").with(csrf()))
            .andExpect(redirectedUrl("/admin/students"));
        assertTrue(students.existsById("SF13S1"));
    }

    @Test @WithMockUser(roles = "ADMIN") void bothFormsRenderTheThreeDropdowns() throws Exception {
        Department[] d = structure("SF14C", "SF14E");
        Section s = academic.sectionsFor(d[0].getId(), 1).get(0);
        admin.createStudent("SF14S1", "Edit me", null, d[0].getId(), 1, s.getId(), 80.0, 7.0);
        mvc.perform(get("/admin/students")).andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"departmentId\"")))
            .andExpect(content().string(containsString("id=\"yearOfStudy\"")))
            .andExpect(content().string(containsString("id=\"sectionId\"")))
            .andExpect(content().string(containsString("1st Year")))
            .andExpect(content().string(containsString("4th Year")));
        mvc.perform(get("/admin/students/SF14S1/edit")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Section A")))
            .andExpect(content().string(containsString("Section B")));      // the student's own department and year
    }
}
