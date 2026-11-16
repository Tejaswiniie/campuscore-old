package com.campuscore;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** A faculty member's classes and the students in them. Only classes assigned to that faculty member are visible. */
@Service
public class FacultyClassService {
    private final TeachingAssignmentRepository teaching;
    private final StudentRepository students;

    public FacultyClassService(TeachingAssignmentRepository teaching, StudentRepository students) {
        this.teaching = teaching;
        this.students = students;
    }

    public List<ClassRow> classesOf(String facultyId) {
        List<ClassRow> out = new ArrayList<>();
        for (TeachingAssignment t : teaching.forFaculty(facultyId)) {
            out.add(new ClassRow(t, students.countBySectionId(t.getSection().getId())));
        }
        return out;
    }

    /** Returns the class only if it is assigned to this faculty member, otherwise refuses. */
    public TeachingAssignment requireClass(String facultyId, Long teachingId) {
        TeachingAssignment t = teachingId == null ? null : teaching.findById(teachingId).orElse(null);
        if (t == null || !t.getFaculty().getFacultyId().equals(facultyId)) {
            throw new AccessDeniedException("This class is not assigned to you.");
        }
        return t;
    }

    /** Students of one of the faculty member's own classes (read only). */
    public List<Student> studentsOf(String facultyId, Long teachingId, String q) {
        TeachingAssignment t = requireClass(facultyId, teachingId);
        String term = q == null ? "" : q.trim();
        Long sectionId = t.getSection().getId();
        return term.isEmpty() ? students.findBySectionIdOrderByRollNoAsc(sectionId)
                              : students.searchInSection(sectionId, term);
    }
}
