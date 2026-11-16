package com.campuscore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FeatureAccessTest {

    @Autowired MockMvc mvc;

    @Test @WithMockUser(roles = "STUDENT") void studentOpensComplaintBox() throws Exception {
        mvc.perform(get("/student/complaints")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "STUDENT") void studentOpensIdeaHub() throws Exception {
        mvc.perform(get("/student/ideas")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "FACULTY") void facultyOpensIdeaHub() throws Exception {
        mvc.perform(get("/faculty/ideas")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN") void adminOpensComplaints() throws Exception {
        mvc.perform(get("/admin/complaints")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "FACULTY") void facultyCannotOpenTheComplaintBox() throws Exception {
        mvc.perform(get("/student/complaints")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "STUDENT") void studentCannotReadAdminComplaints() throws Exception {
        mvc.perform(get("/admin/complaints")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "STUDENT") void studentCannotReviewIdeas() throws Exception {
        mvc.perform(get("/faculty/ideas")).andExpect(status().isForbidden());
    }
}
