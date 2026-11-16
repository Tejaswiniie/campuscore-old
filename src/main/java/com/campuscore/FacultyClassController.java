package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Faculty: assigned classes and the students in them (read only). */
@Controller
@RequestMapping("/faculty/classes")
public class FacultyClassController {
    private final FacultyClassService service;
    private final NameLookup names;

    public FacultyClassController(FacultyClassService service, NameLookup names) {
        this.service = service;
        this.names = names;
    }

    @GetMapping
    public String classes(Model model, Authentication auth) {
        fill(model, auth);
        model.addAttribute("classes", service.classesOf(auth.getName()));
        return "faculty-classes";
    }

    @GetMapping("/{id}")
    public String students(@PathVariable("id") Long id,
                           @RequestParam(name = "q", required = false) String q,
                           Model model, Authentication auth) {
        TeachingAssignment cls = service.requireClass(auth.getName(), id);   // 403 if not this faculty's class
        fill(model, auth);
        model.addAttribute("cls", cls);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("list", service.studentsOf(auth.getName(), id, q));
        return "faculty-class";
    }

    private void fill(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.faculty(auth.getName()));
    }
}
