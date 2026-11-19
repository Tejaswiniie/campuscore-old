package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.Locale;

/** Landing pages: sign in, access denied and one dashboard per role. */
@Controller
public class PageController {
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final DepartmentRepository departments;
    private final ComplaintService complaints;
    private final StudentPortalService portal;
    private final FacultyClassService classes;
    private final NameLookup names;
    private final AssignmentService assignmentService;

    public PageController(StudentRepository students, FacultyRepository faculty,
                          DepartmentRepository departments, ComplaintService complaints,
                          StudentPortalService portal, FacultyClassService classes, NameLookup names,
                          AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
        this.students = students;
        this.faculty = faculty;
        this.departments = departments;
        this.complaints = complaints;
        this.portal = portal;
        this.classes = classes;
        this.names = names;
    }

    @GetMapping("/")
    public String root(Authentication auth) {
        return "redirect:" + RoleRedirect.target(auth.getAuthorities());
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/denied")
    public String denied() {
        return "denied";
    }

    @GetMapping("/admin")
    public String admin(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", "Administrator");
        model.addAttribute("studentCount", students.count());
        model.addAttribute("facultyCount", faculty.count());
        model.addAttribute("departmentCount", departments.count());
        model.addAttribute("openComplaints", complaints.openCount());
        return "admin";
    }

    @GetMapping("/faculty")
    public String faculty(Model model, Authentication auth) {
        Faculty f = faculty.findById(auth.getName()).orElseGet(Faculty::new);
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.faculty(auth.getName()));
        model.addAttribute("faculty", f);
        List<ClassRow> rows = classes.classesOf(auth.getName());
        long studentTotal = 0;
        for (ClassRow row : rows) studentTotal += row.getStudentCount();
        model.addAttribute("classes", rows);
        model.addAttribute("classCount", rows.size());
        model.addAttribute("studentTotal", studentTotal);
        model.addAttribute("assignmentCount", assignmentService.countPostedBy(auth.getName()));
        return "faculty";
    }

    @GetMapping("/student")
    public String student(Model model, Authentication auth) {
        Student s = portal.profile(auth.getName());
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.student(auth.getName()));
        model.addAttribute("student", s);
        boolean hasRecord = s.getRollNo() != null;
        model.addAttribute("attendance", s.getAttendance());
        model.addAttribute("zone", AttendanceZone.of(s.getAttendance()));
        model.addAttribute("cgpa", s.getCgpa());
        model.addAttribute("cgpaZone", CgpaZone.of(s.getCgpa()));
        // Everything the rings show comes from the stored values. Without a student record they show a neutral ring.
        model.addAttribute("attendanceZone", hasRecord ? AttendanceZone.of(s.getAttendance()) : "none");
        model.addAttribute("attendanceText", hasRecord ? String.format(Locale.ROOT, "%.1f%%", s.getAttendance()) : "-");
        model.addAttribute("attendancePercent", hasRecord ? Gauge.percent(s.getAttendance(), 100) : 0.0);
        model.addAttribute("attendanceLabel", hasRecord ? AttendanceZone.label(s.getAttendance()) : "No data");
        model.addAttribute("cgpaRingZone", hasRecord ? CgpaZone.of(s.getCgpa()) : "none");
        model.addAttribute("cgpaText", hasRecord ? String.format(Locale.ROOT, "%.2f", s.getCgpa()) : "-");
        model.addAttribute("cgpaPercent", hasRecord ? Gauge.percent(s.getCgpa(), 10) : 0.0);
        model.addAttribute("cgpaLabel", hasRecord ? CgpaZone.label(s.getCgpa()) : "No data");
        return "student";
    }
}
