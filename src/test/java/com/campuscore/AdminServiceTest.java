package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class AdminServiceTest {

    @Autowired AdminService service;
    @Autowired AppUserRepository users;
    @Autowired StudentRepository students;
    @Autowired FacultyRepository faculty;
    @Autowired ComplaintService complaints;
    @Autowired PasswordEncoder encoder;
    @Autowired Fixtures fx;

    @Test void newStudentLogsInWithRollNumberAsPassword() {
        fx.student("TEST001", fx.section("AS1", 2, "A"));
        AppUser u = users.findById("TEST001").orElseThrow();
        assertEquals("STUDENT", u.getRole());
        assertTrue(encoder.matches("TEST001", u.getPasswordHash()));
    }

    @Test void newFacultyLogsInWithFacultyIdAsPassword() {
        fx.faculty("TESTF01", fx.dept("AS1"));
        AppUser u = users.findById("TESTF01").orElseThrow();
        assertEquals("FACULTY", u.getRole());
        assertTrue(encoder.matches("TESTF01", u.getPasswordHash()));
    }

    @Test void duplicateRollNumberIsRejected() {
        Section sec = fx.section("AS1", 2, "A");
        fx.student("TEST002", sec);
        assertThrows(IllegalArgumentException.class, () -> fx.student("TEST002", sec));
    }

    @Test void duplicateFacultyIdIsRejected() {
        Department d = fx.dept("AS1");
        fx.faculty("TESTF03", d);
        assertThrows(IllegalArgumentException.class, () -> fx.faculty("TESTF03", d));
    }

    @Test void adminIdCannotBeReusedAsRollNumber() {
        Section sec = fx.section("AS1", 2, "A");
        assertThrows(IllegalArgumentException.class, () -> fx.student("admin", sec));
    }

    @Test void facultyIdCannotReuseAStudentRollNumber() {
        fx.student("TEST003", fx.section("AS1", 2, "A"));
        Department d = fx.dept("AS1");
        assertThrows(IllegalArgumentException.class, () -> fx.faculty("TEST003", d));
    }

    @Test void studentMustBelongToAValidSection() {
        assertThrows(IllegalArgumentException.class,
            () -> service.createStudent("TEST005", "No Section", null, 80.0, 7.0));
        assertThrows(IllegalArgumentException.class,
            () -> service.createStudent("TEST005", "Bad Section", 999999L, 80.0, 7.0));
    }

    @Test void attendanceAbove100AndCgpaAbove10AreRejected() {
        Section sec = fx.section("AS1", 2, "A");
        assertThrows(IllegalArgumentException.class,
            () -> service.createStudent("TEST004", "X", sec.getId(), 101.0, 7.0));
        assertThrows(IllegalArgumentException.class,
            () -> service.createStudent("TEST004", "X", sec.getId(), 90.0, 10.5));
        assertThrows(IllegalArgumentException.class,
            () -> service.createStudent("TEST004", "X", sec.getId(), Double.NaN, 7.0));
    }

    @Test void idWithUnsafeCharactersIsRejected() {
        Section sec = fx.section("AS1", 2, "A");
        assertThrows(IllegalArgumentException.class,
            () -> service.createStudent("A/B", "X", sec.getId(), 80.0, 7.0));
    }

    @Test void adminCanEditAStudent() {
        Section a = fx.section("AS1", 2, "A");
        Section b = fx.section("AS1", 2, "B");
        fx.student("TEST006", a);
        service.updateStudent("TEST006", "New Name", b.getId(), 55.5, 6.2);
        Student s = students.findById("TEST006").orElseThrow();
        assertEquals("New Name", s.getName());
        assertEquals(b.getId(), s.getSection().getId());
        assertEquals(55.5, s.getAttendance());
        assertEquals(6.2, s.getCgpa());
    }

    @Test void adminCanEditFaculty() {
        Department d1 = fx.dept("AS1");
        Department d2 = fx.dept("AS2");
        fx.faculty("TESTF04", d1);
        service.updateFaculty("TESTF04", "Renamed", d2.getId(), "Dean");
        Faculty f = faculty.findById("TESTF04").orElseThrow();
        assertEquals("Renamed", f.getName());
        assertEquals(d2.getId(), f.getDepartment().getId());
        assertEquals("Dean", f.getDesignation());
    }

    @Test void editingAnUnknownStudentFails() {
        Section sec = fx.section("AS1", 2, "A");
        assertThrows(IllegalArgumentException.class,
            () -> service.updateStudent("NOPE", "X", sec.getId(), 80.0, 7.0));
    }

    @Test void deletingStudentRemovesTheLoginAndTheirComplaints() {
        fx.student("TEST010", fx.section("AS1", 2, "A"));
        complaints.submit("TEST010", "Hostel", "Water", "No water in the hostel");
        service.deleteStudent("TEST010");
        assertFalse(users.existsById("TEST010"));
        assertFalse(students.existsById("TEST010"));
        assertTrue(complaints.forStudent("TEST010").isEmpty());
    }

    @Test void deletingStudentDoesNotTouchAFacultyLogin() {
        fx.faculty("TESTF02", fx.dept("AS1"));
        service.deleteStudent("TESTF02");
        assertTrue(users.existsById("TESTF02"));
    }

    @Test void deletingFacultyRemovesLoginClassesAndAssignments() {
        Department d = fx.dept("AS1");
        Section sec = fx.section("AS1", 2, "A");
        Faculty f = fx.faculty("TESTF05", d);
        Subject sub = fx.subject("ASSUB1", d);
        fx.teach(f, sub, sec);
        service.deleteFaculty("TESTF05");
        assertFalse(users.existsById("TESTF05"));
        assertFalse(faculty.existsById("TESTF05"));
    }
}
