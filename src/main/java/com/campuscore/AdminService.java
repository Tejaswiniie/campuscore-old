package com.campuscore;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates students and faculty. The initial password is the ID itself. */
@Service
public class AdminService {
    private final AppUserRepository users;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final PasswordEncoder encoder;

    public AdminService(AppUserRepository users, StudentRepository students,
                        FacultyRepository faculty, PasswordEncoder encoder) {
        this.users = users;
        this.students = students;
        this.faculty = faculty;
        this.encoder = encoder;
    }

    @Transactional
    public void createStudent(Student s) {
        String id = clean(s.getRollNo());
        if (id.isEmpty() || clean(s.getName()).isEmpty()) {
            throw new IllegalArgumentException("Roll number and name are required.");
        }
        if (users.existsById(id)) {
            throw new IllegalArgumentException("The ID " + id + " is already registered.");
        }
        if (s.getAttendance() < 0 || s.getAttendance() > 100) {
            throw new IllegalArgumentException("Attendance must be between 0 and 100.");
        }
        if (s.getCgpa() < 0 || s.getCgpa() > 10) {
            throw new IllegalArgumentException("CGPA must be between 0 and 10.");
        }
        s.setRollNo(id);
        students.save(s);
        users.save(new AppUser(id, encoder.encode(id), "STUDENT"));
    }

    @Transactional
    public void createFaculty(Faculty f) {
        String id = clean(f.getFacultyId());
        if (id.isEmpty() || clean(f.getName()).isEmpty()) {
            throw new IllegalArgumentException("Faculty ID and name are required.");
        }
        if (users.existsById(id)) {
            throw new IllegalArgumentException("The ID " + id + " is already registered.");
        }
        f.setFacultyId(id);
        faculty.save(f);
        users.save(new AppUser(id, encoder.encode(id), "FACULTY"));
    }

    private static String clean(String x) {
        return x == null ? "" : x.trim();
    }
}
