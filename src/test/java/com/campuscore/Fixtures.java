package com.campuscore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Test helper that builds departments, sections, students, faculty and classes (found by component scan in tests). */
@Component
class Fixtures {
    @Autowired AcademicService academic;
    @Autowired AdminService admin;
    @Autowired DepartmentRepository departments;
    @Autowired SectionRepository sections;
    @Autowired StudentRepository students;
    @Autowired FacultyRepository faculty;
    @Autowired SubjectRepository subjects;

    Department dept(String code) {
        return departments.findByCodeIgnoreCase(code)
            .orElseGet(() -> academic.createDepartment(code, "Department " + code));
    }

    Section section(String deptCode, int year, String name) {
        Department d = dept(deptCode);
        return sections.findByDepartmentIdAndYearOfStudyAndNameIgnoreCase(d.getId(), year, name)
            .orElseGet(() -> academic.createSection(d.getId(), year, name));
    }

    Student student(String roll, Section s) {
        return admin.createStudent(roll, "Student " + roll, s.getId(), 80.0, 7.5);
    }

    Faculty faculty(String id, Department d) {
        return admin.createFaculty(id, "Faculty " + id, d.getId(), "Professor");
    }

    Subject subject(String code, Department d) {
        return academic.createSubject(code, "Subject " + code, d.getId());
    }

    TeachingAssignment teach(Faculty f, Subject sub, Section sec) {
        return academic.assignTeacher(f.getFacultyId(), sub.getId(), sec.getId());
    }
}
