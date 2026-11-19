package com.campuscore;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ContentDisposition;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Upload and download through the real web layer: forms, security, headers and the original file name. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AssignmentAttachmentWebTest {

    @Autowired MockMvc mvc;
    @Autowired Fixtures fx;
    @Autowired AssignmentService service;
    @Autowired AssignmentRepository assignments;

    private final LocalDate due = LocalDate.now().plusDays(7);

    private TeachingAssignment classFor(String tag, String facultyId) {
        Department d = fx.dept(tag);
        Section sec = fx.section(tag, 3, "A");
        return fx.teach(fx.faculty(facultyId, d), fx.subject(tag + "S", d), sec);
    }

    private Assignment withFile(TeachingAssignment t, String facultyId, String name) {
        return service.create(facultyId, t.getId(), "Task", "Do it", due, TestFiles.file(name, TestFiles.pdf()));
    }

    // ---------- upload ----------

    @Test @WithMockUser(username = "AW1F1", roles = "FACULTY") void facultyUploadsAFileThroughTheForm() throws Exception {
        TeachingAssignment t = classFor("AW1", "AW1F1");
        mvc.perform(multipart("/faculty/assignments").file(TestFiles.file("notes.pdf", TestFiles.pdf()))
                .param("teachingId", String.valueOf(t.getId())).param("title", "Week 1")
                .param("description", "Read it").param("dueDate", due.toString()).with(csrf()))
            .andExpect(redirectedUrl("/faculty/assignments"));
        Assignment a = service.forFaculty("AW1F1").get(0);
        assertTrue(a.isAttached());
        assertEquals("notes.pdf", a.getAttachmentOriginalName());
    }

    @Test @WithMockUser(username = "AW2F1", roles = "FACULTY") void anUnsupportedFileShowsAClearMessageAndCreatesNothing() throws Exception {
        TeachingAssignment t = classFor("AW2", "AW2F1");
        mvc.perform(multipart("/faculty/assignments").file(TestFiles.file("tool.exe", TestFiles.pdf()))
                .param("teachingId", String.valueOf(t.getId())).param("title", "Bad")
                .param("description", "x").param("dueDate", due.toString()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Only PDF, DOCX, PPTX, XLSX and TXT")));
        assertEquals(0, service.forFaculty("AW2F1").size());
    }

    @Test @WithMockUser(username = "AW3F1", roles = "FACULTY") void theFormStillWorksWithoutAnyFile() throws Exception {
        TeachingAssignment t = classFor("AW3", "AW3F1");
        mvc.perform(multipart("/faculty/assignments")
                .param("teachingId", String.valueOf(t.getId())).param("title", "Plain")
                .param("description", "no file").param("dueDate", due.toString()).with(csrf()))
            .andExpect(redirectedUrl("/faculty/assignments"));
        assertFalse(service.forFaculty("AW3F1").get(0).isAttached());
    }

    @Test @WithMockUser(username = "AW4F1", roles = "FACULTY") void editFormReplacesAndRemovesTheAttachment() throws Exception {
        TeachingAssignment t = classFor("AW4", "AW4F1");
        Assignment a = withFile(t, "AW4F1", "first.pdf");
        mvc.perform(multipart("/faculty/assignments/{id}/edit", a.getId()).file(TestFiles.file("second.pdf", TestFiles.pdf()))
                .param("teachingId", String.valueOf(t.getId())).param("title", "Task")
                .param("description", "Do it").param("dueDate", due.toString()).with(csrf()))
            .andExpect(redirectedUrl("/faculty/assignments"));
        assertEquals("second.pdf", assignments.findById(a.getId()).orElseThrow().getAttachmentOriginalName());

        mvc.perform(multipart("/faculty/assignments/{id}/edit", a.getId())
                .param("teachingId", String.valueOf(t.getId())).param("title", "Task")
                .param("description", "Do it").param("dueDate", due.toString())
                .param("removeAttachment", "true").with(csrf()))
            .andExpect(redirectedUrl("/faculty/assignments"));
        Assignment now = assignments.findById(a.getId()).orElseThrow();
        assertFalse(now.isAttached());
        assertEquals("Task", now.getTitle());
    }

    // ---------- download ----------

    @Test @WithMockUser(username = "AW5S1", roles = "STUDENT") void studentDownloadsWithTheOriginalFileName() throws Exception {
        TeachingAssignment t = classFor("AW5", "AW5F1");
        fx.student("AW5S1", t.getSection());
        Assignment a = withFile(t, "AW5F1", "Unit 1 Notes.pdf");
        MvcResult result = mvc.perform(get("/student/assignments/{id}/attachment", a.getId()))
            .andExpect(status().isOk()).andReturn();
        String header = result.getResponse().getHeader("Content-Disposition");
        assertTrue(header.startsWith("attachment"));
        assertEquals("Unit 1 Notes.pdf", ContentDisposition.parse(header).getFilename());
        assertEquals("application/pdf", result.getResponse().getContentType());
        assertEquals("nosniff", result.getResponse().getHeader("X-Content-Type-Options"));
        assertArrayEquals(TestFiles.pdf(), result.getResponse().getContentAsByteArray());
    }

    @Test @WithMockUser(username = "AW6S1", roles = "STUDENT") void studentOfAnotherSectionCannotDownloadByGuessingTheId() throws Exception {
        TeachingAssignment t = classFor("AW6", "AW6F1");
        fx.student("AW6S1", fx.section("AW6", 3, "B"));
        Assignment a = withFile(t, "AW6F1", "secret.pdf");
        mvc.perform(get("/student/assignments/{id}/attachment", a.getId())).andExpect(status().isForbidden());
        mvc.perform(get("/student/assignments/{id}/attachment", 987654321L)).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username = "AW7F1", roles = "FACULTY") void facultyDownloadsTheirOwnAttachment() throws Exception {
        TeachingAssignment t = classFor("AW7", "AW7F1");
        Assignment a = withFile(t, "AW7F1", "mine.pdf");
        mvc.perform(get("/faculty/assignments/{id}/attachment", a.getId())).andExpect(status().isOk());
    }

    @Test @WithMockUser(username = "AW8F2", roles = "FACULTY") void anotherFacultyMemberCannotDownloadOrOpenTheEditPage() throws Exception {
        TeachingAssignment t = classFor("AW8", "AW8F1");
        fx.faculty("AW8F2", fx.dept("AW8"));
        Assignment a = withFile(t, "AW8F1", "private.pdf");
        mvc.perform(get("/faculty/assignments/{id}/attachment", a.getId())).andExpect(status().isForbidden());
        mvc.perform(get("/faculty/assignments/{id}/edit", a.getId())).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN") void theAdminAccountIsNotAFacultyOrStudentAndGetsNoDownload() throws Exception {
        mvc.perform(get("/faculty/assignments/1/attachment")).andExpect(status().isForbidden());
        mvc.perform(get("/student/assignments/1/attachment")).andExpect(status().isForbidden());
    }

    @Test void anonymousUsersAreSentToTheLoginPage() throws Exception {
        mvc.perform(get("/student/assignments/1/attachment")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/faculty/assignments/1/attachment")).andExpect(status().is3xxRedirection());
    }

    // ---------- pages ----------

    @Test @WithMockUser(username = "AW9F1", roles = "FACULTY") void facultyPageShowsTheAttachmentIndicator() throws Exception {
        TeachingAssignment t = classFor("AW9", "AW9F1");
        withFile(t, "AW9F1", "Syllabus.pdf");
        service.create("AW9F1", t.getId(), "No file task", "D", due);
        mvc.perform(get("/faculty/assignments")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Syllabus.pdf")))
            .andExpect(content().string(containsString("Download")))
            .andExpect(content().string(containsString("No attachment")));
    }

    @Test @WithMockUser(username = "AW10S1", roles = "STUDENT") void studentPageShowsADownloadButtonOrTheNoAttachmentText() throws Exception {
        TeachingAssignment t = classFor("AW10", "AW10F1");
        fx.student("AW10S1", t.getSection());
        Assignment a = withFile(t, "AW10F1", "Reading list.pdf");
        mvc.perform(get("/student/assignments")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Reading list.pdf")))
            .andExpect(content().string(containsString("/student/assignments/" + a.getId() + "/attachment")));
        service.update("AW10F1", a.getId(), t.getId(), "Task", "Do it", due, null, true);
        mvc.perform(get("/student/assignments")).andExpect(status().isOk())
            .andExpect(content().string(containsString("No attachment available")));
    }
}
