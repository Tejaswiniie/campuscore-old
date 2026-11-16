package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/complaints")
public class StudentComplaintController {
    private final ComplaintService service;
    private final NameLookup names;

    public StudentComplaintController(ComplaintService service, NameLookup names) {
        this.service = service;
        this.names = names;
    }

    @GetMapping
    public String page(Model model, Authentication auth) {
        fill(model, auth);
        return "student-complaints";
    }

    @PostMapping
    public String submit(@RequestParam("category") String category,
                         @RequestParam("subject") String subject,
                         @RequestParam("description") String description,
                         Model model, RedirectAttributes redirect, Authentication auth) {
        try {
            service.submit(auth.getName(), category, subject, description);
            redirect.addFlashAttribute("message", "Complaint submitted. You can track its status below.");
            return "redirect:/student/complaints";
        } catch (IllegalArgumentException e) {
            fill(model, auth);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("category", category);
            model.addAttribute("subject", subject);
            model.addAttribute("description", description);
            return "student-complaints";
        }
    }

    private void fill(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.student(auth.getName()));
        model.addAttribute("categories", ComplaintService.CATEGORIES);
        model.addAttribute("list", service.forStudent(auth.getName()));
    }
}
