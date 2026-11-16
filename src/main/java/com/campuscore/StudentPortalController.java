package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Student: assignments for their own section and information about their faculty and mentors. */
@Controller
@RequestMapping("/student")
public class StudentPortalController {
    private final AssignmentService assignments;
    private final StudentPortalService portal;
    private final IdeaService ideas;
    private final NameLookup names;

    public StudentPortalController(AssignmentService assignments, StudentPortalService portal,
                                   IdeaService ideas, NameLookup names) {
        this.assignments = assignments;
        this.portal = portal;
        this.ideas = ideas;
        this.names = names;
    }

    @GetMapping("/assignments")
    public String assignments(Model model, Authentication auth) {
        fill(model, auth);
        model.addAttribute("list", assignments.visibleToStudent(auth.getName()));
        return "student-assignments";
    }

    @GetMapping("/faculty")
    public String faculty(Model model, Authentication auth) {
        fill(model, auth);
        model.addAttribute("teachers", portal.teachers(auth.getName()));
        model.addAttribute("mentored", ideas.joinedApproved(auth.getName()));
        return "student-faculty";
    }

    private void fill(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.student(auth.getName()));
    }
}
