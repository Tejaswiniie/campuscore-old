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

@Controller
@RequestMapping("/student/ideas")
public class StudentIdeaController {
    private final IdeaService service;
    private final NameLookup names;

    public StudentIdeaController(IdeaService service, NameLookup names) {
        this.service = service;
        this.names = names;
    }

    @GetMapping
    public String page(Model model, Authentication auth) {
        fill(model, auth);
        return "student-ideas";
    }

    @PostMapping
    public String post(@RequestParam("title") String title,
                       @RequestParam("description") String description,
                       Model model, RedirectAttributes redirect, Authentication auth) {
        try {
            service.post(auth.getName(), title, description);
            redirect.addFlashAttribute("message", "Idea posted. It is visible to others once a faculty member approves it.");
            return "redirect:/student/ideas";
        } catch (IllegalArgumentException e) {
            fill(model, auth);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", title);
            model.addAttribute("description", description);
            return "student-ideas";
        }
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable("id") Long id, RedirectAttributes redirect, Authentication auth) {
        try {
            service.join(id, auth.getName());
            redirect.addFlashAttribute("message", "You joined this idea.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/ideas";
    }

    private void fill(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.student(auth.getName()));
        model.addAttribute("approved", service.approvedCards(auth.getName()));
        model.addAttribute("mine", service.myIdeas(auth.getName()));
    }
}
