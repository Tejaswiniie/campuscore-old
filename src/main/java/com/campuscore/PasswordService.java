package com.campuscore;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lets a signed-in student or faculty member change their own password. */
@Service
public class PasswordService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public PasswordService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public void change(String username, String current, String next, String confirm) {
        AppUser u = users.findById(username).orElseThrow(() -> new IllegalArgumentException("Account not found."));
        if (current == null || !encoder.matches(current, u.getPasswordHash())) {
            throw new IllegalArgumentException("The current password is not correct.");
        }
        if (next == null || next.length() < 8) {
            throw new IllegalArgumentException("The new password must be at least 8 characters.");
        }
        if (next.length() > 72) {
            throw new IllegalArgumentException("The new password is too long (72 characters at most).");
        }
        if (!next.equals(confirm)) {
            throw new IllegalArgumentException("The new password and its confirmation do not match.");
        }
        if (next.equals(current)) {
            throw new IllegalArgumentException("Choose a password different from the current one.");
        }
        if (next.equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("The password cannot be the same as your ID.");
        }
        u.setPasswordHash(encoder.encode(next));
        users.save(u);
    }
}
