package com.campuscore;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads the login (ID, password hash, role) from the database. */
@Service
public class DbUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;

    public DbUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser a = users.findById(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.withUsername(a.getUsername())
            .password(a.getPasswordHash())
            .roles(a.getRole())
            .build();
    }
}
