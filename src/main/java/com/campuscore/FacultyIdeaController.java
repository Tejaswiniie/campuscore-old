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
@RequestMapping("/faculty/ideas")
public class FacultyIdeaController {
    private final IdeaService service;
    private final NameLookup names;

    public FacultyIdeaController(IdeaService service, NameLookup names) {
        this.service = service;
        this.names = names;
    }

    @GetMapping
    public String page(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.faculty(auth.getName()));
        model.addAttribute("pending", service.pending());
        model.addAttribute("approved", service.approvedCards(null));
        return "faculty-ideas";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") Long id, RedirectAttributes redirect, Authentication auth) {
        try {
            service.approve(id, auth.getName());
            redirect.addFlashAttribute("message", "Idea approved. Students can now join it.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/ideas";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id,
                         @RequestParam(name = "reason", required = false) String reason,
                         RedirectAttributes redirect, Authentication auth) {
        try {
            service.reject(id, auth.getName(), reason);
            redirect.addFlashAttribute("message", "Idea rejected.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/ideas";
    }
}
