package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin: Department, Sections, Students overview, plus subjects and who teaches which class. */
@Controller
@RequestMapping("/admin")
public class AdminAcademicController {
    private final AcademicService academic;
    private final FacultyRepository faculty;

    public AdminAcademicController(AcademicService academic, FacultyRepository faculty) {
        this.academic = academic;
        this.faculty = faculty;
    }

    // ---------- departments ----------

    @GetMapping("/departments")
    public String departments(Model model, Authentication auth) {
        sidebar(model, auth);
        model.addAttribute("rows", academic.departmentRows());
        return "admin-departments";
    }

    // ---------- sections of one department ----------

    @GetMapping("/departments/{id}")
    public String department(@PathVariable("id") Long id, Model model, Authentication auth,
                             RedirectAttributes redirect) {
        Department d;
        try {
            d = academic.department(id);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/departments";
        }
        sidebar(model, auth);
        model.addAttribute("department", d);
        model.addAttribute("rows", academic.sectionRows(id));
        return "admin-department";
    }

    @PostMapping("/departments/{id}/sections")
    public String addSection(@PathVariable("id") Long id,
                             @RequestParam(name = "year", required = false) Integer year,
                             @RequestParam("name") String name, RedirectAttributes redirect) {
        try {
            Section s = academic.createSection(id, year, name);
            redirect.addFlashAttribute("message", "Section " + s.getName() + " of " + s.getYearLabel() + " created.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments/" + id;
    }

    @PostMapping("/sections/{id}/delete")
    public String deleteSection(@PathVariable("id") Long id, RedirectAttributes redirect) {
        Long departmentId;
        try {
            departmentId = academic.section(id).getDepartment().getId();
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/departments";
        }
        try {
            academic.deleteSection(id);
            redirect.addFlashAttribute("message", "Section deleted.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments/" + departmentId;
    }

    // ---------- students of one section ----------

    @GetMapping("/sections/{id}")
    public String section(@PathVariable("id") Long id,
                          @RequestParam(name = "q", required = false) String q,
                          Model model, Authentication auth, RedirectAttributes redirect) {
        Section s;
        try {
            s = academic.section(id);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/departments";
        }
        sidebar(model, auth);
        model.addAttribute("section", s);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("list", academic.studentsOfSection(id, q));
        return "admin-section";
    }

    // ---------- subjects and classes ----------

    @GetMapping("/classes")
    public String classes(Model model, Authentication auth) {
        sidebar(model, auth);
        model.addAttribute("departments", academic.departments());
        model.addAttribute("subjects", academic.allSubjects());
        model.addAttribute("sections", academic.allSections());
        model.addAttribute("facultyList", faculty.findAllByOrderByFacultyIdAsc());
        model.addAttribute("teaching", academic.allTeaching());
        return "admin-classes";
    }

    @PostMapping("/subjects")
    public String addSubject(@RequestParam("code") String code, @RequestParam("name") String name,
                             @RequestParam(name = "departmentId", required = false) Long departmentId,
                             RedirectAttributes redirect) {
        try {
            Subject s = academic.createSubject(code, name, departmentId);
            redirect.addFlashAttribute("message", "Subject " + s.getCode() + " created.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @PostMapping("/subjects/{id}/delete")
    public String deleteSubject(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            academic.deleteSubject(id);
            redirect.addFlashAttribute("message", "Subject deleted.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @PostMapping("/teaching")
    public String assignTeacher(@RequestParam(name = "facultyId", required = false) String facultyId,
                                @RequestParam(name = "subjectId", required = false) Long subjectId,
                                @RequestParam(name = "sectionId", required = false) Long sectionId,
                                RedirectAttributes redirect) {
        try {
            academic.assignTeacher(facultyId, subjectId, sectionId);
            redirect.addFlashAttribute("message", "Class assigned.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @PostMapping("/teaching/{id}/delete")
    public String removeTeaching(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            academic.removeTeaching(id);
            redirect.addFlashAttribute("message", "Class removed.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    private static void sidebar(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", "Administrator");
    }
}
