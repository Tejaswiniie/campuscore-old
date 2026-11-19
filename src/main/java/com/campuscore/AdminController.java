package com.campuscore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin: student and faculty master records (add, view, search, edit, delete). */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminService service;
    private final AcademicService academic;
    private final StudentRepository students;
    private final FacultyRepository faculty;

    public AdminController(AdminService service, AcademicService academic,
                           StudentRepository students, FacultyRepository faculty) {
        this.service = service;
        this.academic = academic;
        this.students = students;
        this.faculty = faculty;
    }

    @GetMapping("/login")
    public String login() {
        return "admin-login";
    }

    // ---------- students ----------

    @GetMapping("/students")
    public String students(@RequestParam(name = "q", required = false) String q, Model model, Authentication auth) {
        model.addAttribute("form", new StudentForm());
        studentPage(model, auth, q);
        return "admin-students";
    }

    @PostMapping("/students")
    public String addStudent(@ModelAttribute("form") StudentForm form, BindingResult binding,
                             Model model, RedirectAttributes redirect, Authentication auth) {
        String error = binding.hasErrors() ? "Check the number and date fields." : null;
        if (error == null) {
            try {
                service.createStudent(form.getRollNo(), form.getName(), form.getDob(), form.getDepartmentId(),
                                      form.getYearOfStudy(), form.getSectionId(),
                                      form.getAttendance(), form.getCgpa());
                redirect.addFlashAttribute("message", "Student " + form.getRollNo().trim()
                    + " created. The initial password is the roll number.");
                return "redirect:/admin/students";
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }
        studentPage(model, auth, null);
        model.addAttribute("sectionOptions", academic.sectionsFor(form.getDepartmentId(), form.getYearOfStudy()));
        model.addAttribute("error", error);
        return "admin-students";
    }

    @GetMapping("/students/{rollNo}/edit")
    public String editStudent(@PathVariable("rollNo") String rollNo, Model model,
                              RedirectAttributes redirect, Authentication auth) {
        Student s = students.findById(rollNo).orElse(null);
        if (s == null) {
            redirect.addFlashAttribute("error", "Student not found.");
            return "redirect:/admin/students";
        }
        StudentForm f = new StudentForm();
        f.setRollNo(s.getRollNo());
        f.setName(s.getName());
        f.setDob(s.getDob());
        f.setDepartmentId(s.getSection().getDepartment().getId());
        f.setYearOfStudy(s.getSection().getYearOfStudy());
        f.setSectionId(s.getSection().getId());
        f.setAttendance(s.getAttendance());
        f.setCgpa(s.getCgpa());
        model.addAttribute("form", f);
        editStudentPage(model, auth, rollNo, f);
        return "admin-student-edit";
    }

    @PostMapping("/students/{rollNo}/edit")
    public String updateStudent(@PathVariable("rollNo") String rollNo,
                                @ModelAttribute("form") StudentForm form, BindingResult binding,
                                Model model, RedirectAttributes redirect, Authentication auth) {
        String error = binding.hasErrors() ? "Check the number and date fields." : null;
        if (error == null) {
            try {
                service.updateStudent(rollNo, form.getName(), form.getDob(), form.getDepartmentId(),
                                      form.getYearOfStudy(), form.getSectionId(),
                                      form.getAttendance(), form.getCgpa());
                redirect.addFlashAttribute("message", "Student " + rollNo + " updated.");
                return "redirect:/admin/students";
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }
        editStudentPage(model, auth, rollNo, form);
        model.addAttribute("error", error);
        return "admin-student-edit";
    }

    @PostMapping("/students/{rollNo}/delete")
    public String deleteStudent(@PathVariable("rollNo") String rollNo, RedirectAttributes redirect) {
        service.deleteStudent(rollNo);
        redirect.addFlashAttribute("message", "Student " + rollNo + " deleted.");
        return "redirect:/admin/students";
    }

    /** Sections the admin registered for a department and year, as JSON for the student form. Admin only. */
    @GetMapping("/section-options")
    @ResponseBody
    public List<SectionOption> sectionOptions(@RequestParam("departmentId") Long departmentId,
                                              @RequestParam("year") Integer year) {
        List<SectionOption> options = new ArrayList<>();
        for (Section s : academic.sectionsFor(departmentId, year)) {
            options.add(new SectionOption(s.getId(), s.getName()));
        }
        return options;
    }

    // ---------- faculty ----------

    @GetMapping("/faculty")
    public String faculty(@RequestParam(name = "q", required = false) String q, Model model, Authentication auth) {
        model.addAttribute("form", new FacultyForm());
        facultyPage(model, auth, q);
        return "admin-faculty";
    }

    @PostMapping("/faculty")
    public String addFaculty(@ModelAttribute("form") FacultyForm form, BindingResult binding,
                             Model model, RedirectAttributes redirect, Authentication auth) {
        String error = binding.hasErrors() ? "Check the fields." : null;
        if (error == null) {
            try {
                service.createFaculty(form.getFacultyId(), form.getName(), form.getDepartmentId(), form.getDesignation());
                redirect.addFlashAttribute("message", "Faculty " + form.getFacultyId().trim()
                    + " created. The initial password is the faculty ID.");
                return "redirect:/admin/faculty";
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }
        facultyPage(model, auth, null);
        model.addAttribute("error", error);
        return "admin-faculty";
    }

    @GetMapping("/faculty/{facultyId}/edit")
    public String editFaculty(@PathVariable("facultyId") String facultyId, Model model,
                              RedirectAttributes redirect, Authentication auth) {
        Faculty f = faculty.findById(facultyId).orElse(null);
        if (f == null) {
            redirect.addFlashAttribute("error", "Faculty member not found.");
            return "redirect:/admin/faculty";
        }
        FacultyForm form = new FacultyForm();
        form.setFacultyId(f.getFacultyId());
        form.setName(f.getName());
        form.setDepartmentId(f.getDepartment().getId());
        form.setDesignation(f.getDesignation());
        model.addAttribute("form", form);
        editFacultyPage(model, auth, facultyId);
        return "admin-faculty-edit";
    }

    @PostMapping("/faculty/{facultyId}/edit")
    public String updateFaculty(@PathVariable("facultyId") String facultyId,
                                @ModelAttribute("form") FacultyForm form, BindingResult binding,
                                Model model, RedirectAttributes redirect, Authentication auth) {
        String error = binding.hasErrors() ? "Check the fields." : null;
        if (error == null) {
            try {
                service.updateFaculty(facultyId, form.getName(), form.getDepartmentId(), form.getDesignation());
                redirect.addFlashAttribute("message", "Faculty " + facultyId + " updated.");
                return "redirect:/admin/faculty";
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }
        editFacultyPage(model, auth, facultyId);
        model.addAttribute("error", error);
        return "admin-faculty-edit";
    }

    @PostMapping("/faculty/{facultyId}/delete")
    public String deleteFaculty(@PathVariable("facultyId") String facultyId, RedirectAttributes redirect) {
        service.deleteFaculty(facultyId);
        redirect.addFlashAttribute("message", "Faculty " + facultyId + " deleted.");
        return "redirect:/admin/faculty";
    }

    // ---------- page helpers ----------

    private void studentPage(Model model, Authentication auth, String q) {
        sidebar(model, auth);
        String term = q == null ? "" : q.trim();
        model.addAttribute("q", term);
        model.addAttribute("sections", academic.allSections());
        model.addAttribute("departments", academic.departments());
        model.addAttribute("years", yearOptions());
        model.addAttribute("sectionOptions", new ArrayList<Section>());
        model.addAttribute("list", term.isEmpty() ? students.findAllByOrderByRollNoAsc() : students.search(term));
    }

    private void editStudentPage(Model model, Authentication auth, String rollNo, StudentForm form) {
        sidebar(model, auth);
        model.addAttribute("rollNo", rollNo);
        model.addAttribute("departments", academic.departments());
        model.addAttribute("years", yearOptions());
        model.addAttribute("sectionOptions", academic.sectionsFor(form.getDepartmentId(), form.getYearOfStudy()));
    }

    /** The four fixed academic years. They are not stored anywhere; a section carries its year. */
    private static Map<Integer, String> yearOptions() {
        Map<Integer, String> years = new LinkedHashMap<>();
        for (int y = 1; y <= 4; y++) years.put(y, Section.yearName(y));
        return years;
    }

    private void facultyPage(Model model, Authentication auth, String q) {
        sidebar(model, auth);
        String term = q == null ? "" : q.trim();
        model.addAttribute("q", term);
        model.addAttribute("departments", academic.departments());
        model.addAttribute("list", term.isEmpty() ? faculty.findAllByOrderByFacultyIdAsc() : faculty.search(term));
    }

    private void editFacultyPage(Model model, Authentication auth, String facultyId) {
        sidebar(model, auth);
        model.addAttribute("facultyId", facultyId);
        model.addAttribute("departments", academic.departments());
    }

    private static void sidebar(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", "Administrator");
    }
}
