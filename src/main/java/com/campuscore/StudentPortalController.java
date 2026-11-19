package com.campuscore;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

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

    /** Downloads the attachment of an assignment of the student's own section; anything else is refused with 403. */
    @GetMapping("/assignments/{id}/attachment")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id, Authentication auth) {
        try {
            return Downloads.of(assignments.attachmentForStudent(auth.getName(), id));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
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
