package com.campuscore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates only the admin account at startup. Students and faculty are added by the admin. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public DataSeeder(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (!users.existsById("admin")) {
            users.save(new AppUser("admin", encoder.encode("admin123"), "ADMIN"));
        }
    }
}
