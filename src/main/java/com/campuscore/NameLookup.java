package com.campuscore;

import org.springframework.stereotype.Component;

/** Finds the display name for a signed-in student or faculty member. Falls back to the ID. */
@Component
public class NameLookup {
    private final StudentRepository students;
    private final FacultyRepository faculty;

    public NameLookup(StudentRepository students, FacultyRepository faculty) {
        this.students = students;
        this.faculty = faculty;
    }

    public String student(String roll) {
        return students.findById(roll).map(Student::getName)
            .filter(n -> !n.isBlank()).orElse(roll);
    }

    public String faculty(String id) {
        return faculty.findById(id).map(Faculty::getName)
            .filter(n -> !n.isBlank()).orElse(id);
    }
}
