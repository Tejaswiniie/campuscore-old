package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StudentDobTest {

    @Autowired AdminService service;
    @Autowired StudentRepository students;
    @Autowired Fixtures fx;

    @Test void dateOfBirthIsStoredWhenGiven() {
        Section sec = fx.section("DOB1", 2, "A");
        service.createStudent("DOB001", "Student DOB001", LocalDate.of(2005, 8, 12), sec.getId(), 80.0, 7.5);
        assertEquals(LocalDate.of(2005, 8, 12), students.findById("DOB001").orElseThrow().getDob());
    }

    @Test void dateOfBirthIsOptional() {
        Section sec = fx.section("DOB1", 2, "A");
        fx.student("DOB002", sec);
        assertNull(students.findById("DOB002").orElseThrow().getDob());
    }

    @Test void futureDateOfBirthIsRejected() {
        Section sec = fx.section("DOB1", 2, "A");
        assertThrows(IllegalArgumentException.class, () ->
            service.createStudent("DOB003", "X", LocalDate.now().plusDays(1), sec.getId(), 80.0, 7.5));
    }

    @Test void shortUpdateKeepsTheDateOfBirthAndFullUpdateChangesIt() {
        Section sec = fx.section("DOB1", 2, "A");
        service.createStudent("DOB004", "Student DOB004", LocalDate.of(2004, 1, 2), sec.getId(), 80.0, 7.5);
        service.updateStudent("DOB004", "Student DOB004", sec.getId(), 81.0, 7.6);
        assertEquals(LocalDate.of(2004, 1, 2), students.findById("DOB004").orElseThrow().getDob());
        service.updateStudent("DOB004", "Student DOB004", LocalDate.of(2003, 5, 6), sec.getId(), 81.0, 7.6);
        assertEquals(LocalDate.of(2003, 5, 6), students.findById("DOB004").orElseThrow().getDob());
    }
}
