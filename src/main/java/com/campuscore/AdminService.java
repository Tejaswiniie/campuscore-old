package com.campuscore;

import java.time.LocalDate;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, edits and deletes students and faculty. The initial password is the ID itself. */
@Service
public class AdminService {
    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,30}$");

    private final AppUserRepository users;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final DepartmentRepository departments;
    private final SectionRepository sections;
    private final TeachingAssignmentRepository teaching;
    private final AssignmentRepository assignments;
    private final ComplaintRepository complaints;
    private final IdeaMemberRepository ideaMembers;
    private final PasswordEncoder encoder;
    private final AssignmentService assignmentService;

    public AdminService(AppUserRepository users, StudentRepository students, FacultyRepository faculty,
                        DepartmentRepository departments, SectionRepository sections,
                        TeachingAssignmentRepository teaching, AssignmentRepository assignments,
                        ComplaintRepository complaints, IdeaMemberRepository ideaMembers,
                        PasswordEncoder encoder, AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
        this.users = users;
        this.students = students;
        this.faculty = faculty;
        this.departments = departments;
        this.sections = sections;
        this.teaching = teaching;
        this.assignments = assignments;
        this.complaints = complaints;
        this.ideaMembers = ideaMembers;
        this.encoder = encoder;
    }

    // ---- students ----

    /** The date of birth is optional, but when given it must be a real past date. */
    private static void checkDob(LocalDate dob) {
        if (dob != null && (dob.isAfter(LocalDate.now()) || dob.getYear() < 1950)) {
            throw new IllegalArgumentException("Enter a valid date of birth.");
        }
    }

    /** Creates a student after checking that the section belongs to the chosen department and year. */
    @Transactional
    public Student createStudent(String rollNo, String name, LocalDate dob, Long departmentId,
                                 Integer yearOfStudy, Long sectionId, Double attendance, Double cgpa) {
        checkSectionMatches(departmentId, yearOfStudy, sectionId);
        return createStudent(rollNo, name, dob, sectionId, attendance, cgpa);
    }

    /** Creates a student without a date of birth. */
    @Transactional
    public Student createStudent(String rollNo, String name, Long sectionId, Double attendance, Double cgpa) {
        return createStudent(rollNo, name, null, sectionId, attendance, cgpa);
    }

    @Transactional
    public Student createStudent(String rollNo, String name, LocalDate dob, Long sectionId,
                                 Double attendance, Double cgpa) {
        String id = newId(rollNo, "Roll number");
        String n = requireName(name);
        Section section = requireSection(sectionId);
        checkMarks(attendance, cgpa);
        checkDob(dob);
        Student s = new Student();
        s.setRollNo(id);
        s.setName(n);
        s.setDob(dob);
        s.setSection(section);
        s.setAttendance(attendance);
        s.setCgpa(cgpa);
        Student saved = students.save(s);
        users.save(new AppUser(id, encoder.encode(id), "STUDENT"));
        return saved;
    }

    /** Updates a student after checking that the section belongs to the chosen department and year. */
    @Transactional
    public Student updateStudent(String rollNo, String name, LocalDate dob, Long departmentId,
                                 Integer yearOfStudy, Long sectionId, Double attendance, Double cgpa) {
        checkSectionMatches(departmentId, yearOfStudy, sectionId);
        return updateStudent(rollNo, name, dob, sectionId, attendance, cgpa);
    }

    /** Updates a student and keeps the date of birth that is already stored. */
    @Transactional
    public Student updateStudent(String rollNo, String name, Long sectionId, Double attendance, Double cgpa) {
        Student current = students.findById(clean(rollNo))
            .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        return updateStudent(rollNo, name, current.getDob(), sectionId, attendance, cgpa);
    }

    @Transactional
    public Student updateStudent(String rollNo, String name, LocalDate dob, Long sectionId,
                                 Double attendance, Double cgpa) {
        Student s = students.findById(clean(rollNo))
            .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        String n = requireName(name);
        Section section = requireSection(sectionId);
        checkMarks(attendance, cgpa);
        checkDob(dob);
        s.setName(n);
        s.setDob(dob);
        s.setSection(section);
        s.setAttendance(attendance);
        s.setCgpa(cgpa);
        return students.save(s);
    }

    /** Removes the student, the login, their complaints and their idea memberships. Does nothing if the ID is not a student. */
    @Transactional
    public void deleteStudent(String rollNo) {
        if (students.existsById(rollNo)) {
            complaints.deleteByStudentRoll(rollNo);
            ideaMembers.deleteByStudentRoll(rollNo);
            students.deleteById(rollNo);
            users.deleteById(rollNo);
        }
    }

    // ---- faculty ----

    @Transactional
    public Faculty createFaculty(String facultyId, String name, Long departmentId, String designation) {
        String id = newId(facultyId, "Faculty ID");
        String n = requireName(name);
        Department d = requireDepartment(departmentId);
        String des = requireDesignation(designation);
        Faculty f = new Faculty();
        f.setFacultyId(id);
        f.setName(n);
        f.setDepartment(d);
        f.setDesignation(des);
        Faculty saved = faculty.save(f);
        users.save(new AppUser(id, encoder.encode(id), "FACULTY"));
        return saved;
    }

    @Transactional
    public Faculty updateFaculty(String facultyId, String name, Long departmentId, String designation) {
        Faculty f = faculty.findById(clean(facultyId))
            .orElseThrow(() -> new IllegalArgumentException("Faculty member not found."));
        String n = requireName(name);
        Department d = requireDepartment(departmentId);
        String des = requireDesignation(designation);
        f.setName(n);
        f.setDepartment(d);
        f.setDesignation(des);
        return faculty.save(f);
    }

    /** Removes the faculty member, the login, their classes and their assignments. Does nothing if the ID is not faculty. */
    @Transactional
    public void deleteFaculty(String facultyId) {
        if (faculty.existsById(facultyId)) {
            assignmentService.deleteAllOf(facultyId);
            teaching.deleteByFacultyFacultyId(facultyId);
            faculty.deleteById(facultyId);
            users.deleteById(facultyId);
        }
    }

    // ---- validation helpers ----

    private String newId(String raw, String label) {
        String id = clean(raw);
        if (id.isEmpty()) throw new IllegalArgumentException(label + " is required.");
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(label + " may use letters, digits, hyphen and underscore only (30 characters at most).");
        }
        if (id.equalsIgnoreCase("admin") || users.existsById(id)) {
            throw new IllegalArgumentException("The ID " + id + " is already registered.");
        }
        return id;
    }

    private String requireName(String name) {
        String n = clean(name);
        if (n.isEmpty()) throw new IllegalArgumentException("Name is required.");
        if (n.length() > 100) throw new IllegalArgumentException("The name is too long (100 characters at most).");
        return n;
    }

    private String requireDesignation(String designation) {
        String d = clean(designation);
        if (d.length() > 100) throw new IllegalArgumentException("The designation is too long (100 characters at most).");
        return d.isEmpty() ? null : d;
    }

    /** The server never trusts the drop-downs: the section must belong to the department and year sent with it. */
    private void checkSectionMatches(Long departmentId, Integer yearOfStudy, Long sectionId) {
        if (departmentId == null) throw new IllegalArgumentException("Choose a department.");
        if (yearOfStudy == null || yearOfStudy < 1 || yearOfStudy > 4) {
            throw new IllegalArgumentException("Choose the academic year (1st to 4th year).");
        }
        Section s = requireSection(sectionId);
        if (!s.getDepartment().getId().equals(departmentId)) {
            throw new IllegalArgumentException("The selected section does not belong to the selected department.");
        }
        if (s.getYearOfStudy() != yearOfStudy) {
            throw new IllegalArgumentException("The selected section does not belong to the selected academic year.");
        }
    }

    private Section requireSection(Long sectionId) {
        if (sectionId == null) throw new IllegalArgumentException("Choose a section.");
        return sections.findById(sectionId).orElseThrow(() -> new IllegalArgumentException("Choose a valid section."));
    }

    private Department requireDepartment(Long departmentId) {
        if (departmentId == null) throw new IllegalArgumentException("Choose a department.");
        return departments.findById(departmentId).orElseThrow(() -> new IllegalArgumentException("Choose a valid department."));
    }

    private static void checkMarks(Double attendance, Double cgpa) {
        if (attendance == null || !(attendance >= 0 && attendance <= 100)) {
            throw new IllegalArgumentException("Attendance must be between 0 and 100.");
        }
        if (cgpa == null || !(cgpa >= 0 && cgpa <= 10)) {
            throw new IllegalArgumentException("CGPA must be between 0 and 10.");
        }
    }

    private static String clean(String x) {
        return x == null ? "" : x.trim();
    }
}
