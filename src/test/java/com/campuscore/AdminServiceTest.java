package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminServiceTest {

    @Autowired AdminService service;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    private Student student(String roll) {
        Student s = new Student();
        s.setRollNo(roll);
        s.setName("Test Student");
        s.setDepartment("CSE");
        s.setYearOfStudy(2);
        s.setSection("A");
        s.setAttendance(80);
        s.setCgpa(7.5);
        return s;
    }

    private Faculty faculty(String id) {
        Faculty f = new Faculty();
        f.setFacultyId(id);
        f.setName("Test Faculty");
        f.setDepartment("CSE");
        return f;
    }

    @Test void newStudentLogsInWithRollNumberAsPassword() {
        service.createStudent(student("TEST001"));
        AppUser u = users.findById("TEST001").orElseThrow();
        assertEquals("STUDENT", u.getRole());
        assertTrue(encoder.matches("TEST001", u.getPasswordHash()));
    }

    @Test void newFacultyLogsInWithFacultyIdAsPassword() {
        service.createFaculty(faculty("TESTF01"));
        AppUser u = users.findById("TESTF01").orElseThrow();
        assertEquals("FACULTY", u.getRole());
        assertTrue(encoder.matches("TESTF01", u.getPasswordHash()));
    }

    @Test void duplicateRollNumberIsRejected() {
        service.createStudent(student("TEST002"));
        assertThrows(IllegalArgumentException.class, () -> service.createStudent(student("TEST002")));
    }

    @Test void seededStudentCannotBeCreatedAgain() {
        assertThrows(IllegalArgumentException.class, () -> service.createStudent(student("22CS001")));
    }

    @Test void facultyIdCannotReuseAStudentRollNumber() {
        service.createStudent(student("TEST003"));
        assertThrows(IllegalArgumentException.class, () -> service.createFaculty(faculty("TEST003")));
    }

    @Test void attendanceAbove100IsRejected() {
        Student s = student("TEST004");
        s.setAttendance(101);
        assertThrows(IllegalArgumentException.class, () -> service.createStudent(s));
    }
}
