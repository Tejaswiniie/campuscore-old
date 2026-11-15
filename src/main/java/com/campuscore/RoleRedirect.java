package com.campuscore;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public final class RoleRedirect {
    private RoleRedirect() {}

    public static String target(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority a : authorities) {
            if (a.getAuthority().equals("ROLE_ADMIN")) return "/admin";
            if (a.getAuthority().equals("ROLE_FACULTY")) return "/faculty";
        }
        return "/student";
    }
}
