package com.campuscore;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Date of birth through the real web layer: admin form, admin edit page and the student's own profile. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentDobWebTest {

    @Autowired MockMvc mvc;
    @Autowired Fixtures fx;
    @Autowired AdminService admin;
    @Autowired StudentRepository students;

    @Test @WithMockUser(roles = "ADMIN") void adminAddsADateOfBirthThroughTheForm() throws Exception {
        Section sec = fx.section("DOBW", 2, "A");
        mvc.perform(post("/admin/students").param("rollNo", "DOBW1").param("name", "Dob Student")
                .param("dob", "2005-08-12").param("departmentId", String.valueOf(sec.getDepartment().getId()))
                .param("yearOfStudy", String.valueOf(sec.getYearOfStudy())).param("sectionId", String.valueOf(sec.getId()))
                .param("attendance", "81.5").param("cgpa", "7.9").with(csrf()))
            .andExpect(redirectedUrl("/admin/students"));
        assertEquals(LocalDate.of(2005, 8, 12), students.findById("DOBW1").orElseThrow().getDob());
    }

    @Test @WithMockUser(username = "DOBW2", roles = "STUDENT") void studentSeesTheirOwnDateOfBirth() throws Exception {
        Section sec = fx.section("DOBW", 2, "A");
        admin.createStudent("DOBW2", "Student DOBW2", LocalDate.of(2004, 3, 9), sec.getId(), 80.0, 7.5);
        mvc.perform(get("/student")).andExpect(content().string(containsString("2004-03-09")));
    }

    @Test @WithMockUser(roles = "ADMIN") void adminEditPageShowsTheStoredDateOfBirth() throws Exception {
        Section sec = fx.section("DOBW", 2, "A");
        admin.createStudent("DOBW3", "Student DOBW3", LocalDate.of(2002, 11, 30), sec.getId(), 80.0, 7.5);
        mvc.perform(get("/admin/students/DOBW3/edit")).andExpect(content().string(containsString("2002-11-30")));
    }
}
