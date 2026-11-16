package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Faculty: create, edit and delete their own assignments for the classes they teach. */
@Controller
@RequestMapping("/faculty/assignments")
public class FacultyAssignmentController {
    private final AssignmentService service;
    private final FacultyClassService classes;
    private final NameLookup names;

    public FacultyAssignmentController(AssignmentService service, FacultyClassService classes, NameLookup names) {
        this.service = service;
        this.classes = classes;
        this.names = names;
    }

    @GetMapping
    public String page(Model model, Authentication auth) {
        model.addAttribute("form", new AssignmentForm());
        fill(model, auth);
        return "faculty-assignments";
    }

    @PostMapping
    public String create(@ModelAttribute("form") AssignmentForm form, BindingResult binding,
                         Model model, RedirectAttributes redirect, Authentication auth) {
        String error = binding.hasErrors() ? "Check the due date." : null;
        if (error == null) {
            try {
                service.create(auth.getName(), form.getTeachingId(), form.getTitle(),
                               form.getDescription(), form.getDueDate());
                redirect.addFlashAttribute("message", "Assignment created.");
                return "redirect:/faculty/assignments";
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }
        fill(model, auth);
        model.addAttribute("error", error);
        return "faculty-assignments";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model, RedirectAttributes redirect, Authentication auth) {
        Assignment a;
        try {
            a = service.owned(auth.getName(), id);      // 403 if it belongs to another faculty member
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/faculty/assignments";
        }
        AssignmentForm form = new AssignmentForm();
        form.setTitle(a.getTitle());
        form.setDescription(a.getDescription());
        form.setDueDate(a.getDueDate());
        for (ClassRow row : classes.classesOf(auth.getName())) {
            TeachingAssignment t = row.getTeaching();
            if (t.getSubject().getId().equals(a.getSubject().getId())
                    && t.getSection().getId().equals(a.getSection().getId())) {
                form.setTeachingId(t.getId());
            }
        }
        model.addAttribute("form", form);
        editFill(model, auth, id);
        return "faculty-assignment-edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable("id") Long id, @ModelAttribute("form") AssignmentForm form,
                         BindingResult binding, Model model, RedirectAttributes redirect, Authentication auth) {
        String error = binding.hasErrors() ? "Check the due date." : null;
        if (error == null) {
            try {
                service.update(auth.getName(), id, form.getTeachingId(), form.getTitle(),
                               form.getDescription(), form.getDueDate());
                redirect.addFlashAttribute("message", "Assignment updated.");
                return "redirect:/faculty/assignments";
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }
        editFill(model, auth, id);
        model.addAttribute("error", error);
        return "faculty-assignment-edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirect, Authentication auth) {
        try {
            service.delete(auth.getName(), id);
            redirect.addFlashAttribute("message", "Assignment deleted.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/assignments";
    }

    private void fill(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.faculty(auth.getName()));
        model.addAttribute("classes", classes.classesOf(auth.getName()));
        model.addAttribute("list", service.forFaculty(auth.getName()));
    }

    private void editFill(Model model, Authentication auth, Long assignmentId) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.faculty(auth.getName()));
        model.addAttribute("classes", classes.classesOf(auth.getName()));
        model.addAttribute("assignmentId", assignmentId);
    }
}
