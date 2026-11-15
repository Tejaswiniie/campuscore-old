package com.campuscore;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CrossLoginTest {

    @Autowired MockMvc mvc;
    @Autowired AdminService service;
    @Autowired AppUserRepository users;

    @BeforeEach void createTestUsers() {
        if (!users.existsById("XSTU1")) {
            Student s = new Student();
            s.setRollNo("XSTU1");
            s.setName("Test Student");
            service.createStudent(s);
        }
        if (!users.existsById("XFAC1")) {
            Faculty f = new Faculty();
            f.setFacultyId("XFAC1");
            f.setName("Test Faculty");
            service.createFaculty(f);
        }
    }

    @Test void adminLoginPageIsPublic() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk());
    }

    @Test void adminSignsInAtAdminLogin() throws Exception {
        mvc.perform(formLogin("/admin/login").user("admin").password("admin123"))
            .andExpect(redirectedUrl("/admin"));
    }

    @Test void studentSignsInAtPortalLogin() throws Exception {
        mvc.perform(formLogin("/login").user("XSTU1").password("XSTU1"))
            .andExpect(redirectedUrl("/student"));
    }

    @Test void facultySignsInAtPortalLogin() throws Exception {
        mvc.perform(formLogin("/login").user("XFAC1").password("XFAC1"))
            .andExpect(redirectedUrl("/faculty"));
    }

    @Test void studentIsRefusedAtAdminLogin() throws Exception {
        mvc.perform(formLogin("/admin/login").user("XSTU1").password("XSTU1"))
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
