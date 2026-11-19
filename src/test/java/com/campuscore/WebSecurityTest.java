package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Logins, role restrictions, page rendering and ownership checks through the real web layer. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired Fixtures fx;
    @Autowired AdminService admin;
    @Autowired AssignmentService assignments;
    @Autowired ComplaintService complaints;
    @Autowired StudentRepository students;
    @Autowired AppUserRepository users;

    private Section section;
    private Faculty f1;
    private Faculty f2;
    private TeachingAssignment class1;

    @BeforeEach void fixtures() {
        Department d = fx.dept("WEB");
        section = fx.section("WEB", 2, "A");
        fx.student("WS1", section);
        fx.student("WS2", fx.section("WEB", 3, "B"));
        f1 = fx.faculty("WF1", d);
        f2 = fx.faculty("WF2", d);
        class1 = fx.teach(f1, fx.subject("WEBSUB1", d), section);
    }

    // ---------- authentication ----------

    @Test void adminLoginPageIsPublic() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk());
    }

    @Test void adminSignsInAtAdminLogin() throws Exception {
        mvc.perform(formLogin("/admin/login").user("admin").password("admin123")).andExpect(redirectedUrl("/admin"));
    }

    @Test void studentSignsInAtPortalLogin() throws Exception {
        mvc.perform(formLogin("/login").user("WS1").password("WS1")).andExpect(redirectedUrl("/student"));
    }

    @Test void facultySignsInAtPortalLogin() throws Exception {
        mvc.perform(formLogin("/login").user("WF1").password("WF1")).andExpect(redirectedUrl("/faculty"));
    }

    @Test void studentIsRefusedAtAdminLogin() throws Exception {
        mvc.perform(formLogin("/admin/login").user("WS1").password("WS1")).andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test void adminIsRefusedAtPortalLogin() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("admin123")).andExpect(redirectedUrl("/login?error"));
    }

    @Test void wrongPasswordIsRefused() throws Exception {
        mvc.perform(formLogin("/admin/login").user("admin").password("wrong")).andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test void loginPageIsPublicAndHealthEndpointIsOpen() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------- unauthenticated and cross-role access ----------

    @Test void anonymousUsersAreSentToTheRightLoginPage() throws Exception {
        mvc.perform(get("/student")).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/faculty")).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/admin")).andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test @WithMockUser(roles = "STUDENT") void studentCannotOpenAdminOrFacultyPages() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/students")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/complaints")).andExpect(status().isForbidden());
        mvc.perform(get("/faculty")).andExpect(status().isForbidden());
        mvc.perform(get("/faculty/ideas")).andExpect(status().isForbidden());
        mvc.perform(get("/faculty/classes")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "FACULTY") void facultyCannotOpenAdminOrStudentPages() throws Exception {
        mvc.perform(get("/admin/departments")).andExpect(status().isForbidden());
        mvc.perform(get("/student")).andExpect(status().isForbidden());
        mvc.perform(get("/student/complaints")).andExpect(status().isForbidden());
        mvc.perform(get("/student/assignments")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN") void adminCannotOpenStudentOrFacultyPages() throws Exception {
        mvc.perform(get("/student")).andExpect(status().isForbidden());
        mvc.perform(get("/faculty/assignments")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "STUDENT") void studentCannotUseAdminActions() throws Exception {
        mvc.perform(post("/admin/students/WS2/delete").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/departments").param("code", "ZZ").param("name", "Zed").with(csrf()))
            .andExpect(status().isForbidden());
        assertTrue(students.existsById("WS2"));
    }

    @Test @WithMockUser(roles = "FACULTY") void facultyCannotChangeStudentRecords() throws Exception {
        mvc.perform(post("/admin/students/WS1/edit").param("name", "Hacked").param("sectionId", "1")
            .param("attendance", "100").param("cgpa", "10").with(csrf())).andExpect(status().isForbidden());
        assertEquals("Student WS1", students.findById("WS1").orElseThrow().getName());
    }

    @Test @WithMockUser(roles = "ADMIN") void postsWithoutACsrfTokenAreRefused() throws Exception {
        mvc.perform(post("/admin/departments").param("code", "NOCSRF").param("name", "No token"))
            .andExpect(status().isForbidden());
    }

    // ---------- every page renders ----------

    @Test @WithMockUser(roles = "ADMIN") void adminPagesRender() throws Exception {
        for (String p : new String[]{"/admin", "/admin/students", "/admin/faculty", "/admin/departments",
                "/admin/classes", "/admin/complaints", "/admin/students/WS1/edit", "/admin/faculty/WF1/edit",
                "/admin/sections/" + section.getId(), "/admin/departments/" + section.getDepartment().getId()}) {
            mvc.perform(get(p)).andExpect(status().isOk());
        }
    }

    @Test @WithMockUser(username = "WS1", roles = "STUDENT") void studentPagesRender() throws Exception {
        for (String p : new String[]{"/student", "/student/assignments", "/student/faculty", "/student/ideas",
                "/student/complaints", "/student/password"}) {
            mvc.perform(get(p)).andExpect(status().isOk());
        }
    }

    @Test @WithMockUser(username = "WF1", roles = "FACULTY") void facultyPagesRender() throws Exception {
        for (String p : new String[]{"/faculty", "/faculty/classes", "/faculty/classes/" + class1.getId(),
                "/faculty/assignments", "/faculty/ideas", "/faculty/password"}) {
            mvc.perform(get(p)).andExpect(status().isOk());
        }
    }

    // ---------- admin workflows ----------

    @Test @WithMockUser(roles = "ADMIN") void adminCreatesAStudentThroughTheForm() throws Exception {
        mvc.perform(post("/admin/students").param("rollNo", "WNEW1").param("name", "New Student")
                .param("departmentId", String.valueOf(section.getDepartment().getId())).param("yearOfStudy", String.valueOf(section.getYearOfStudy()))
                .param("sectionId", String.valueOf(section.getId())).param("attendance", "81.5").param("cgpa", "7.9")
                .with(csrf()))
            .andExpect(redirectedUrl("/admin/students"));
        assertTrue(students.existsById("WNEW1"));
        assertTrue(users.existsById("WNEW1"));
    }

    @Test @WithMockUser(roles = "ADMIN") void duplicateStudentShowsAnErrorAndCreatesNothingNew() throws Exception {
        mvc.perform(post("/admin/students").param("rollNo", "WS1").param("name", "Again")
                .param("departmentId", String.valueOf(section.getDepartment().getId())).param("yearOfStudy", String.valueOf(section.getYearOfStudy()))
                .param("sectionId", String.valueOf(section.getId())).param("attendance", "81").param("cgpa", "7")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("already registered")));
        assertEquals("Student WS1", students.findById("WS1").orElseThrow().getName());
    }

    @Test @WithMockUser(roles = "ADMIN") void adminSearchesStudents() throws Exception {
        mvc.perform(get("/admin/students").param("q", "WS2"))
            .andExpect(content().string(containsString("Student WS2")))
            .andExpect(content().string(not(containsString("Student WS1"))));
    }

    @Test @WithMockUser(roles = "ADMIN") void adminDepartmentPageShowsCountsAndSections() throws Exception {
        mvc.perform(get("/admin/departments")).andExpect(content().string(containsString("WEB")));
        mvc.perform(get("/admin/departments/" + section.getDepartment().getId()))
            .andExpect(content().string(containsString("View students")));
    }

    @Test @WithMockUser(roles = "ADMIN") void adminManagesComplaints() throws Exception {
        Complaint c = complaints.submit("WS1", "Hostel", "Water supply", "No water since morning");
        mvc.perform(post("/admin/complaints/" + c.getId()).param("status", "Resolved")
                .param("response", "Fixed").with(csrf()))
            .andExpect(redirectedUrl("/admin/complaints"));
        Complaint saved = complaints.forStudent("WS1").get(0);
        assertEquals("Resolved", saved.getStatus());
        assertEquals("Fixed", saved.getResponse());
    }

    @Test @WithMockUser(username = "WS1", roles = "STUDENT") void studentCannotManageComplaints() throws Exception {
        Complaint c = complaints.submit("WS1", "Hostel", "Water supply", "No water since morning");
        mvc.perform(post("/admin/complaints/" + c.getId()).param("status", "Resolved").with(csrf()))
            .andExpect(status().isForbidden());
        assertEquals("Submitted", complaints.forStudent("WS1").get(0).getStatus());
    }

    // ---------- student ownership ----------

    @Test @WithMockUser(username = "WS1", roles = "STUDENT") void studentSeesOnlyTheirOwnProfile() throws Exception {
        mvc.perform(get("/student"))
            .andExpect(content().string(containsString("Student WS1")))
            .andExpect(content().string(not(containsString("Student WS2"))));
    }

    @Test @WithMockUser(username = "WS1", roles = "STUDENT") void studentSeesOnlyTheirOwnComplaints() throws Exception {
        complaints.submit("WS1", "Hostel", "Mine-complaint", "Text");
        complaints.submit("WS2", "Hostel", "Other-complaint", "Text");
        mvc.perform(get("/student/complaints"))
            .andExpect(content().string(containsString("Mine-complaint")))
            .andExpect(content().string(not(containsString("Other-complaint"))));
    }

    @Test @WithMockUser(username = "WS1", roles = "STUDENT") void studentSeesOnlyAssignmentsOfTheirOwnSection() throws Exception {
        Department d = fx.dept("WEB");
        Section other = fx.section("WEB", 3, "B");
        TeachingAssignment otherClass = fx.teach(f1, fx.subject("WEBSUB2", d), other);
        assignments.create("WF1", class1.getId(), "Visible-task", "Text", LocalDate.now().plusDays(3));
        assignments.create("WF1", otherClass.getId(), "Hidden-task", "Text", LocalDate.now().plusDays(3));
        mvc.perform(get("/student/assignments"))
            .andExpect(content().string(containsString("Visible-task")))
            .andExpect(content().string(not(containsString("Hidden-task"))));
    }

    @Test @WithMockUser(username = "WS1", roles = "STUDENT") void studentSeesTheirFaculty() throws Exception {
        mvc.perform(get("/student/faculty")).andExpect(content().string(containsString("Faculty WF1")));
    }

    @Test void studentChangesPasswordAndCanSignInWithTheNewOne() throws Exception {
        mvc.perform(post("/student/password").param("currentPassword", "WS1").param("newPassword", "brandNew123")
                .param("confirmPassword", "brandNew123").with(csrf())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user("WS1").roles("STUDENT")))
            .andExpect(redirectedUrl("/student/password"));
        mvc.perform(formLogin("/login").user("WS1").password("brandNew123")).andExpect(redirectedUrl("/student"));
        mvc.perform(formLogin("/login").user("WS1").password("WS1")).andExpect(redirectedUrl("/login?error"));
    }

    // ---------- faculty authorization ----------

    @Test @WithMockUser(username = "WF1", roles = "FACULTY") void facultySeesStudentsOfTheirOwnClass() throws Exception {
        mvc.perform(get("/faculty/classes/" + class1.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Student WS1")));
    }

    @Test @WithMockUser(username = "WF2", roles = "FACULTY") void facultyCannotOpenAnotherFacultyMembersClass() throws Exception {
        mvc.perform(get("/faculty/classes/" + class1.getId())).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username = "WF1", roles = "FACULTY") void facultyCreatesAnAssignmentThroughTheForm() throws Exception {
        mvc.perform(post("/faculty/assignments").param("teachingId", String.valueOf(class1.getId()))
                .param("title", "Web-made").param("description", "Text")
                .param("dueDate", LocalDate.now().plusDays(4).toString()).with(csrf()))
            .andExpect(redirectedUrl("/faculty/assignments"));
        assertEquals(1, assignments.forFaculty("WF1").size());
    }

    @Test @WithMockUser(username = "WF2", roles = "FACULTY") void facultyCannotEditOrDeleteAnotherFacultyMembersAssignment() throws Exception {
        Assignment a = assignments.create("WF1", class1.getId(), "Owned", "Text", LocalDate.now().plusDays(4));
        mvc.perform(get("/faculty/assignments/" + a.getId() + "/edit")).andExpect(status().isForbidden());
        mvc.perform(post("/faculty/assignments/" + a.getId() + "/delete").with(csrf())).andExpect(status().isForbidden());
        assertEquals(1, assignments.forFaculty("WF1").size());
    }

    @Test @WithMockUser(username = "WF2", roles = "FACULTY") void facultyCannotPostAssignmentsForAClassTheyDoNotTeach() throws Exception {
        mvc.perform(post("/faculty/assignments").param("teachingId", String.valueOf(class1.getId()))
                .param("title", "Sneaky").param("description", "Text")
                .param("dueDate", LocalDate.now().plusDays(4).toString()).with(csrf()))
            .andExpect(status().isForbidden());
        assertTrue(assignments.forFaculty("WF2").isEmpty());
    }
}
