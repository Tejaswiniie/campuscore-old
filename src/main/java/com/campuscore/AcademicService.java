package com.campuscore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Departments, sections, subjects and which faculty member teaches which class. Admin only. */
@Service
public class AcademicService {
    private final DepartmentRepository departments;
    private final SectionRepository sections;
    private final SubjectRepository subjects;
    private final TeachingAssignmentRepository teaching;
    private final AssignmentRepository assignments;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final AssignmentService assignmentService;

    public AcademicService(DepartmentRepository departments, SectionRepository sections,
                           SubjectRepository subjects, TeachingAssignmentRepository teaching,
                           AssignmentRepository assignments, StudentRepository students,
                           FacultyRepository faculty, AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
        this.departments = departments;
        this.sections = sections;
        this.subjects = subjects;
        this.teaching = teaching;
        this.assignments = assignments;
        this.students = students;
        this.faculty = faculty;
    }

    // ---- departments ----

    @Transactional
    public Department createDepartment(String code, String name) {
        String c = clean(code).toUpperCase(Locale.ROOT);
        String n = clean(name);
        if (c.isEmpty() || n.isEmpty()) throw new IllegalArgumentException("Department code and name are required.");
        if (c.length() > 20) throw new IllegalArgumentException("The department code is too long (20 characters at most).");
        if (n.length() > 100) throw new IllegalArgumentException("The department name is too long (100 characters at most).");
        if (departments.existsByCodeIgnoreCase(c)) {
            throw new IllegalArgumentException("A department with the code " + c + " already exists.");
        }
        if (departments.existsByNameIgnoreCase(n)) {
            throw new IllegalArgumentException("A department named " + n + " already exists.");
        }
        Department d = new Department();
        d.setCode(c);
        d.setName(n);
        return departments.save(d);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department d = department(id);
        if (sections.existsByDepartmentId(id) || faculty.existsByDepartmentId(id) || subjects.existsByDepartmentId(id)) {
            throw new IllegalArgumentException("Remove the sections, faculty and subjects of "
                + d.getCode() + " before deleting the department.");
        }
        departments.delete(d);
    }

    public Department department(Long id) {
        if (id == null) throw new IllegalArgumentException("Choose a department.");
        return departments.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found."));
    }

    public List<Department> departments() {
        return departments.findAllByOrderByNameAsc();
    }

    /** Every department with its section count and student count, calculated live. */
    public List<DepartmentRow> departmentRows() {
        List<DepartmentRow> out = new ArrayList<>();
        for (Department d : departments.findAllByOrderByNameAsc()) {
            out.add(new DepartmentRow(d, sections.countByDepartmentId(d.getId()),
                                      students.countByDepartment(d.getId())));
        }
        return out;
    }

    // ---- sections ----

    @Transactional
    public Section createSection(Long departmentId, Integer year, String name) {
        Department d = department(departmentId);
        if (year == null || year < 1 || year > 4) throw new IllegalArgumentException("Choose the year of study (1st to 4th yr).");
        String n = clean(name).toUpperCase(Locale.ROOT);
        if (n.isEmpty()) throw new IllegalArgumentException("Enter a section name.");
        if (n.length() > 20) throw new IllegalArgumentException("The section name is too long (20 characters at most).");
        if (sections.existsByDepartmentIdAndYearOfStudyAndNameIgnoreCase(d.getId(), year, n)) {
            throw new IllegalArgumentException("Section " + n + " of year " + year + " already exists in " + d.getCode() + ".");
        }
        Section s = new Section();
        s.setDepartment(d);
        s.setYearOfStudy(year);
        s.setName(n);
        return sections.save(s);
    }

    @Transactional
    public void deleteSection(Long id) {
        Section s = section(id);
        if (students.existsBySectionId(id) || teaching.existsBySectionId(id) || assignments.existsBySectionId(id)) {
            throw new IllegalArgumentException("This section still has students, classes or assignments. Remove them first.");
        }
        sections.delete(s);
    }

    public Section section(Long id) {
        if (id == null) throw new IllegalArgumentException("Choose a section.");
        return sections.findById(id).orElseThrow(() -> new IllegalArgumentException("Section not found."));
    }

    /** The sections the admin has registered for one department and one year of study. */
    public List<Section> sectionsFor(Long departmentId, Integer year) {
        if (departmentId == null || year == null || year < 1 || year > 4) return List.of();
        return sections.findByDepartmentIdAndYearOfStudyOrderByNameAsc(departmentId, year);
    }

    public List<Section> allSections() {
        return sections.findAllOrdered();
    }

    /** The sections of one department, including sections that have no students. */
    public List<SectionRow> sectionRows(Long departmentId) {
        List<SectionRow> out = new ArrayList<>();
        for (Section s : sections.findByDepartmentIdOrderByYearOfStudyAscNameAsc(departmentId)) {
            out.add(new SectionRow(s, students.countBySectionId(s.getId())));
        }
        return out;
    }

    /** Students of one section, optionally filtered by roll number or name. */
    public List<Student> studentsOfSection(Long sectionId, String q) {
        String term = clean(q);
        return term.isEmpty() ? students.findBySectionIdOrderByRollNoAsc(sectionId)
                              : students.searchInSection(sectionId, term);
    }

    // ---- subjects ----

    @Transactional
    public Subject createSubject(String code, String name, Long departmentId) {
        String c = clean(code).toUpperCase(Locale.ROOT);
        String n = clean(name);
        Department d = department(departmentId);
        if (c.isEmpty() || n.isEmpty()) throw new IllegalArgumentException("Subject code and name are required.");
        if (c.length() > 30) throw new IllegalArgumentException("The subject code is too long (30 characters at most).");
        if (n.length() > 150) throw new IllegalArgumentException("The subject name is too long (150 characters at most).");
        if (subjects.existsByCodeIgnoreCase(c)) {
            throw new IllegalArgumentException("A subject with the code " + c + " already exists.");
        }
        Subject s = new Subject();
        s.setCode(c);
        s.setName(n);
        s.setDepartment(d);
        return subjects.save(s);
    }

    @Transactional
    public void deleteSubject(Long id) {
        Subject s = subject(id);
        if (teaching.existsBySubjectId(id) || assignments.existsBySubjectId(id)) {
            throw new IllegalArgumentException("This subject is still taught in a class. Remove that class first.");
        }
        subjects.delete(s);
    }

    public Subject subject(Long id) {
        if (id == null) throw new IllegalArgumentException("Choose a subject.");
        return subjects.findById(id).orElseThrow(() -> new IllegalArgumentException("Subject not found."));
    }

    public List<Subject> allSubjects() {
        return subjects.findAllByOrderByCodeAsc();
    }

    // ---- teaching (which faculty member teaches which class) ----

    @Transactional
    public TeachingAssignment assignTeacher(String facultyId, Long subjectId, Long sectionId) {
        Faculty f = faculty.findById(clean(facultyId)).orElseThrow(() -> new IllegalArgumentException("Choose a faculty member."));
        Subject sub = subject(subjectId);
        Section sec = section(sectionId);
        if (!sub.getDepartment().getId().equals(sec.getDepartment().getId())) {
            throw new IllegalArgumentException("The subject belongs to a different department than the section.");
        }
        if (teaching.countByFacultyFacultyIdAndSubjectIdAndSectionId(f.getFacultyId(), sub.getId(), sec.getId()) > 0) {
            throw new IllegalArgumentException("This faculty member already teaches that subject to that section.");
        }
        TeachingAssignment t = new TeachingAssignment();
        t.setFaculty(f);
        t.setSubject(sub);
        t.setSection(sec);
        return teaching.save(t);
    }

    /** Removes the class and the assignments the faculty member posted for it. */
    @Transactional
    public void removeTeaching(Long id) {
        TeachingAssignment t = teaching.findById(id).orElseThrow(() -> new IllegalArgumentException("Class not found."));
        assignmentService.deleteForClass(
            t.getFaculty().getFacultyId(), t.getSubject().getId(), t.getSection().getId());
        teaching.delete(t);
    }

    public List<TeachingAssignment> allTeaching() {
        return teaching.findAllOrdered();
    }

    private static String clean(String x) {
        return x == null ? "" : x.trim();
    }
}
