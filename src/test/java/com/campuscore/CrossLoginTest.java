package com.campuscore;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CrossLoginTest {

    @Autowired MockMvc mvc;

    @Test void adminLoginPageIsPublic() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk());
    }

    @Test void adminSignsInAtAdminLogin() throws Exception {
        mvc.perform(formLogin("/admin/login").user("admin").password("admin123"))
            .andExpect(redirectedUrl("/admin"));
    }

    @Test void studentSignsInAtPortalLogin() throws Exception {
        mvc.perform(formLogin("/login").user("22CS001").password("22CS001"))
            .andExpect(redirectedUrl("/student"));
    }

    @Test void facultySignsInAtPortalLogin() throws Exception {
        mvc.perform(formLogin("/login").user("FAC001").password("FAC001"))
            .andExpect(redirectedUrl("/faculty"));
    }

    @Test void studentIsRefusedAtAdminLogin() throws Exception {
        mvc.perform(formLogin("/admin/login").user("22CS001").password("22CS001"))
            .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test void adminIsRefusedAtPortalLogin() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("admin123"))
            .andExpect(redirectedUrl("/login?error"));
    }

    @Test void wrongPasswordIsRefused() throws Exception {
        mvc.perform(formLogin("/admin/login").user("admin").password("wrong"))
            .andExpect(redirectedUrl("/admin/login?error"));
    }
}
