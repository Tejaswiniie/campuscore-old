package com.campuscore;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/** Maps a signed-in user's role to the dashboard they should land on. */
public final class RoleRedirect {
    private RoleRedirect() {}

    public static String target(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority a : authorities) {
            switch (a.getAuthority()) {
                case "ROLE_ADMIN": return "/admin";
                case "ROLE_FACULTY": return "/faculty";
                case "ROLE_STUDENT": return "/student";
                default: break;
            }
        }
        return "/login";
    }
}
