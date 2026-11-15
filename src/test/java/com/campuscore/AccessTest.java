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
class AccessTest {

    @Autowired MockMvc mvc;

    @Test void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test void dashboardNeedsLogin() throws Exception {
        mvc.perform(get("/student")).andExpect(status().is3xxRedirection());
    }

    @Test @WithMockUser(roles = "STUDENT") void studentOpensStudentPage() throws Exception {
        mvc.perform(get("/student")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "STUDENT") void studentCannotOpenAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "FACULTY") void facultyOpensFacultyPage() throws Exception {
        mvc.perform(get("/faculty")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN") void adminOpensAdminPage() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }
}
