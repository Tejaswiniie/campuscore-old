package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    private final StudentRepository students;
    private final FacultyRepository faculty;

    public PageController(StudentRepository students, FacultyRepository faculty) {
        this.students = students;
        this.faculty = faculty;
    }

    @GetMapping("/")
    public String root(Authentication auth) {
        return "redirect:" + RoleRedirect.target(auth.getAuthorities());
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin")
    public String admin(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", "Administrator");
        model.addAttribute("studentCount", students.count());
        model.addAttribute("facultyCount", faculty.count());
        return "admin";
    }

    @GetMapping("/faculty")
    public String faculty(Model model, Authentication auth) {
        Faculty f = faculty.findById(auth.getName()).orElseGet(Faculty::new);
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", name(f.getName(), auth.getName()));
        model.addAttribute("faculty", f);
        return "faculty";
    }

    @GetMapping("/student")
    public String student(Model model, Authentication auth) {
        Student s = students.findById(auth.getName()).orElseGet(Student::new);
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", name(s.getName(), auth.getName()));
        model.addAttribute("student", s);
        model.addAttribute("attendance", s.getAttendance());
        model.addAttribute("zone", AttendanceZone.of(s.getAttendance()));
        model.addAttribute("cgpa", s.getCgpa());
        model.addAttribute("cgpaPercent", s.getCgpa() * 10);
        model.addAttribute("cgpaZone", CgpaZone.of(s.getCgpa()));
        return "student";
    }

    private static String name(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
