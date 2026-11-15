package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminService service;
    private final StudentRepository students;
    private final FacultyRepository faculty;

    public AdminController(AdminService service, StudentRepository students, FacultyRepository faculty) {
        this.service = service;
        this.students = students;
        this.faculty = faculty;
    }

    @GetMapping("/login")
    public String login() {
        return "admin-login";
    }

    @GetMapping("/students")
    public String students(Model model, Authentication auth) {
        sidebar(model, auth);
        model.addAttribute("student", new Student());
        model.addAttribute("list", students.findAll());
        return "admin-students";
    }

    @PostMapping("/students")
    public String addStudent(@ModelAttribute("student") Student student, BindingResult binding,
                             Model model, RedirectAttributes redirect, Authentication auth) {
        sidebar(model, auth);
        String error = binding.hasErrors() ? "Check the date and number fields." : null;
        if (error == null) {
            try {
                service.createStudent(student);
                redirect.addFlashAttribute("message", "Student " + student.getRollNo()
                    + " created. The initial password is the roll number.");
                return "redirect:/admin/students";
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }
        model.addAttribute("error", error);
        model.addAttribute("list", students.findAll());
        return "admin-students";
    }

    @GetMapping("/faculty")
    public String faculty(Model model, Authentication auth) {
        sidebar(model, auth);
        model.addAttribute("faculty", new Faculty());
        model.addAttribute("list", faculty.findAll());
        return "admin-faculty";
    }

    @PostMapping("/faculty")
    public String addFaculty(@ModelAttribute("faculty") Faculty f, BindingResult binding,
                             Model model, RedirectAttributes redirect, Authentication auth) {
        sidebar(model, auth);
        String error = binding.hasErrors() ? "Check the fields." : null;
        if (error == null) {
            try {
                service.createFaculty(f);
                redirect.addFlashAttribute("message", "Faculty " + f.getFacultyId()
                    + " created. The initial password is the faculty ID.");
                return "redirect:/admin/faculty";
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }
        model.addAttribute("error", error);
        model.addAttribute("list", faculty.findAll());
        return "admin-faculty";
    }

    private static void sidebar(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", "Administrator");
    }
}
