package com.campuscore;

import java.util.List;
import org.springframework.stereotype.Service;

/** Read-only views for a student. Every method takes the signed-in student's own roll number. */
@Service
public class StudentPortalService {
    private final StudentRepository students;
    private final TeachingAssignmentRepository teaching;

    public StudentPortalService(StudentRepository students, TeachingAssignmentRepository teaching) {
        this.students = students;
        this.teaching = teaching;
    }

    public Student profile(String rollNo) {
        return students.findById(rollNo).orElseGet(Student::new);
    }

    /** The faculty who teach the student's own section, with their subjects. */
    public List<TeachingAssignment> teachers(String rollNo) {
        Student s = students.findById(rollNo).orElse(null);
        if (s == null || s.getSection() == null) return List.of();
        return teaching.forSection(s.getSection().getId());
    }
}
