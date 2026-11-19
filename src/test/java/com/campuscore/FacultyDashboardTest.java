package com.campuscore;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** The faculty dashboard shows real database counts, not fixed numbers. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FacultyDashboardTest {

    @Autowired MockMvc mvc;
    @Autowired AssignmentService assignments;
    @Autowired Fixtures fx;

    private final LocalDate due = LocalDate.now().plusDays(3);

    @Test @WithMockUser(username = "FD1F1", roles = "FACULTY") void countsComeFromTheDatabase() throws Exception {
        Department d = fx.dept("FD1");
        Section a = fx.section("FD1", 2, "A");
        Section b = fx.section("FD1", 3, "A");
        Faculty f = fx.faculty("FD1F1", d);
        TeachingAssignment first = fx.teach(f, fx.subject("FD1S1", d), a);
        fx.teach(f, fx.subject("FD1S2", d), b);
        fx.student("FD1S1", a);
        fx.student("FD1S2", a);
        fx.student("FD1S3", b);
        assignments.create("FD1F1", first.getId(), "One", "D", due);
        assignments.create("FD1F1", first.getId(), "Two", "D", due);
        assignments.create("FD1F1", first.getId(), "Three", "D", due);

        mvc.perform(get("/faculty")).andExpect(status().isOk())
            .andExpect(content().string(containsString(">2</span><span class=\"stat-label\">Assigned classes")))
            .andExpect(content().string(containsString(">3</span><span class=\"stat-label\">Assignments posted")))
            .andExpect(content().string(containsString(">3</span><span class=\"stat-label\">Students in my classes")))
            .andExpect(content().string(containsString("Welcome, ")))
                .andExpect(content().string(containsString("Faculty FD1F1")))
            .andExpect(content().string(containsString("Department FD1")))
            .andExpect(content().string(containsString("Professor")));
    }

    @Test @WithMockUser(username = "FD2F1", roles = "FACULTY") void aNewFacultyMemberSeesZerosAndAnEmptyState() throws Exception {
        fx.faculty("FD2F1", fx.dept("FD2"));
        mvc.perform(get("/faculty")).andExpect(status().isOk())
            .andExpect(content().string(containsString(">0</span><span class=\"stat-label\">Assigned classes")))
            .andExpect(content().string(containsString(">0</span><span class=\"stat-label\">Assignments posted")))
            .andExpect(content().string(containsString("No classes have been assigned to you yet")));
    }

    @Test @WithMockUser(username = "FD3F1", roles = "FACULTY") void myClassesShowsSubjectYearSectionAndStudentCount() throws Exception {
        Department d = fx.dept("FD3");
        Section sec = fx.section("FD3", 2, "B");
        fx.teach(fx.faculty("FD3F1", d), fx.subject("FD3S1", d), sec);
        fx.student("FD3S1", sec);
        mvc.perform(get("/faculty/classes")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Subject FD3S1")))
            .andExpect(content().string(containsString("Section")))
            .andExpect(content().string(containsString("View students")));
    }

    @Test @WithMockUser(username = "FD4F1", roles = "FACULTY") void everyFacultyPageStillRenders() throws Exception {
        Department d = fx.dept("FD4");
        Section sec = fx.section("FD4", 2, "A");
        TeachingAssignment t = fx.teach(fx.faculty("FD4F1", d), fx.subject("FD4S1", d), sec);
        Assignment a = assignments.create("FD4F1", t.getId(), "Task", "D", due);
        for (String path : new String[]{"/faculty", "/faculty/classes", "/faculty/classes/" + t.getId(),
                "/faculty/assignments", "/faculty/assignments/" + a.getId() + "/edit",
                "/faculty/ideas", "/faculty/password"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }
}
