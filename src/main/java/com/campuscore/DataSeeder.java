package com.campuscore;

import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Demo data so the app is usable straight after start. Initial password = ID (admin: admin123). */
@Component
public class DataSeeder implements CommandLineRunner {
    private final AppUserRepository users;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final PasswordEncoder encoder;

    public DataSeeder(AppUserRepository users, StudentRepository students,
                      FacultyRepository faculty, PasswordEncoder encoder) {
        this.users = users;
        this.students = students;
        this.faculty = faculty;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (!users.existsById("admin")) {
            users.save(new AppUser("admin", encoder.encode("admin123"), "ADMIN"));
        }
        if (!users.existsById("22CS001")) {
            Student s = new Student();
            s.setRollNo("22CS001");
            s.setName("Demo Student");
            s.setDob(LocalDate.of(2004, 5, 15));
            s.setDepartment("CSE");
            s.setYearOfStudy(3);
            s.setSection("A");
            s.setAttendance(68);
            s.setCgpa(8.2);
            students.save(s);
            users.save(new AppUser("22CS001", encoder.encode("22CS001"), "STUDENT"));
        }
        if (!users.existsById("FAC001")) {
            Faculty f = new Faculty();
            f.setFacultyId("FAC001");
            f.setName("Demo Faculty");
            f.setDepartment("CSE");
            f.setDesignation("Assistant Professor");
            f.setQualifications("M.Tech");
            f.setPublications("");
            faculty.save(f);
            users.save(new AppUser("FAC001", encoder.encode("FAC001"), "FACULTY"));
        }
    }
}
