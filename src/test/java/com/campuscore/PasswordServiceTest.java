package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PasswordServiceTest {

    @Autowired PasswordService service;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired Fixtures fx;

    @Test void passwordCanBeChangedAndIsStoredHashed() {
        fx.student("PW1", fx.section("PW", 2, "A"));
        service.change("PW1", "PW1", "newSecret1", "newSecret1");
        String stored = users.findById("PW1").orElseThrow().getPasswordHash();
        assertTrue(encoder.matches("newSecret1", stored));
        assertFalse(encoder.matches("PW1", stored));
        assertFalse(stored.contains("newSecret1"));
    }

    @Test void wrongCurrentPasswordIsRefused() {
        fx.student("PW2", fx.section("PW", 2, "A"));
        assertThrows(IllegalArgumentException.class, () -> service.change("PW2", "wrong", "newSecret1", "newSecret1"));
    }

    @Test void shortOrMismatchedOrReusedPasswordsAreRefused() {
        fx.student("PW3", fx.section("PW", 2, "A"));
        assertThrows(IllegalArgumentException.class, () -> service.change("PW3", "PW3", "short", "short"));
        assertThrows(IllegalArgumentException.class, () -> service.change("PW3", "PW3", "newSecret1", "different1"));
        assertThrows(IllegalArgumentException.class, () -> service.change("PW3", "PW3", "PW3", "PW3"));
    }

    @Test void facultyCanChangePasswordToo() {
        fx.faculty("PWF1", fx.dept("PW"));
        service.change("PWF1", "PWF1", "facultyPass9", "facultyPass9");
        assertTrue(encoder.matches("facultyPass9", users.findById("PWF1").orElseThrow().getPasswordHash()));
    }
}
