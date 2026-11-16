package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class RoleRedirectTest {
    @Test void eachRoleLandsOnItsOwnDashboard() {
        assertEquals("/admin", RoleRedirect.target(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertEquals("/faculty", RoleRedirect.target(List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))));
        assertEquals("/student", RoleRedirect.target(List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }

    @Test void unknownRoleGoesToLogin() {
        assertEquals("/login", RoleRedirect.target(List.of()));
    }
}
