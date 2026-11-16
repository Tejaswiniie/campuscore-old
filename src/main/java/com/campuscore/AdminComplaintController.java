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
@RequestMapping("/admin/complaints")
public class AdminComplaintController {
    private final ComplaintService service;

    public AdminComplaintController(ComplaintService service) {
        this.service = service;
    }

    @GetMapping
    public String page(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("list", service.all());
        model.addAttribute("statuses", ComplaintService.STATUSES);
        return "admin-complaints";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
                         @RequestParam("status") String status,
                         @RequestParam(name = "response", required = false) String response,
                         RedirectAttributes redirect) {
        try {
            service.update(id, status, response);
            redirect.addFlashAttribute("message", "Complaint #" + id + " updated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/complaints";
    }
}
