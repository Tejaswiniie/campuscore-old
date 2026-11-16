package com.campuscore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assignments (classwork). A faculty member can create, edit and delete only their own assignments,
 * and only for classes they teach. Students see the assignments of their own section.
 */
@Service
public class AssignmentService {
    private final AssignmentRepository assignments;
    private final StudentRepository students;
    private final FacultyClassService classes;

    public AssignmentService(AssignmentRepository assignments, StudentRepository students, FacultyClassService classes) {
        this.assignments = assignments;
        this.students = students;
        this.classes = classes;
    }

    @Transactional
    public Assignment create(String facultyId, Long teachingId, String title, String description, LocalDate dueDate) {
        TeachingAssignment cls = classes.requireClass(facultyId, teachingId);
        String t = clean(title);
        String d = clean(description);
        check(t, d, dueDate);
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("The due date cannot be in the past.");
        }
        Assignment a = new Assignment();
        a.setFaculty(cls.getFaculty());
        a.setSubject(cls.getSubject());
        a.setSection(cls.getSection());
        a.setTitle(t);
        a.setDescription(d);
        a.setDueDate(dueDate);
        a.setCreatedAt(LocalDateTime.now());
        return assignments.save(a);
    }

    @Transactional
    public Assignment update(String facultyId, Long assignmentId, Long teachingId,
                             String title, String description, LocalDate dueDate) {
        Assignment a = owned(facultyId, assignmentId);
        TeachingAssignment cls = classes.requireClass(facultyId, teachingId);
        String t = clean(title);
        String d = clean(description);
        check(t, d, dueDate);
        a.setSubject(cls.getSubject());
        a.setSection(cls.getSection());
        a.setTitle(t);
        a.setDescription(d);
        a.setDueDate(dueDate);
        return assignments.save(a);
    }

    @Transactional
    public void delete(String facultyId, Long assignmentId) {
        Assignment a = owned(facultyId, assignmentId);
        assignments.delete(a);
    }

    /** Returns the assignment only if this faculty member created it, otherwise refuses. */
    public Assignment owned(String facultyId, Long assignmentId) {
        Assignment a = assignmentId == null ? null : assignments.findById(assignmentId).orElse(null);
        if (a == null) throw new IllegalArgumentException("Assignment not found.");
        if (!a.getFaculty().getFacultyId().equals(facultyId)) {
            throw new AccessDeniedException("This assignment belongs to another faculty member.");
        }
        return a;
    }

    public List<Assignment> forFaculty(String facultyId) {
        return assignments.findByFacultyFacultyIdOrderByDueDateAscIdAsc(facultyId);
    }

    /** Assignments for the student's own year and section. */
    public List<Assignment> visibleToStudent(String rollNo) {
        Student s = students.findById(rollNo).orElse(null);
        if (s == null || s.getSection() == null) return List.of();
        return assignments.findBySectionIdOrderByDueDateAscIdAsc(s.getSection().getId());
    }

    private static void check(String title, String description, LocalDate dueDate) {
        if (title.isEmpty()) throw new IllegalArgumentException("Enter a title.");
        if (title.length() > 150) throw new IllegalArgumentException("The title is too long (150 characters at most).");
        if (description.isEmpty()) throw new IllegalArgumentException("Enter a description.");
        if (description.length() > 2000) throw new IllegalArgumentException("The description is too long (2000 characters at most).");
        if (dueDate == null) throw new IllegalArgumentException("Choose a due date.");
    }

    private static String clean(String x) {
        return x == null ? "" : x.trim();
    }
}
