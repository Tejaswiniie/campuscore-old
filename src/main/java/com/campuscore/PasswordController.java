package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Students and faculty change their own password. The account is always the signed-in user. */
@Controller
public class PasswordController {
    private final PasswordService service;
    private final NameLookup names;

    public PasswordController(PasswordService service, NameLookup names) {
        this.service = service;
        this.names = names;
    }

    @GetMapping("/student/password")
    public String studentPage(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.student(auth.getName()));
        return "student-password";
    }

    @PostMapping("/student/password")
    public String studentChange(@RequestParam("currentPassword") String current,
                                @RequestParam("newPassword") String next,
                                @RequestParam("confirmPassword") String confirm,
                                RedirectAttributes redirect, Authentication auth) {
        return change(auth, current, next, confirm, redirect, "/student/password");
    }

    @GetMapping("/faculty/password")
    public String facultyPage(Model model, Authentication auth) {
        model.addAttribute("id", auth.getName());
        model.addAttribute("displayName", names.faculty(auth.getName()));
        return "faculty-password";
    }

    @PostMapping("/faculty/password")
    public String facultyChange(@RequestParam("currentPassword") String current,
                                @RequestParam("newPassword") String next,
                                @RequestParam("confirmPassword") String confirm,
                                RedirectAttributes redirect, Authentication auth) {
        return change(auth, current, next, confirm, redirect, "/faculty/password");
    }

    private String change(Authentication auth, String current, String next, String confirm,
                          RedirectAttributes redirect, String page) {
        try {
            service.change(auth.getName(), current, next, confirm);
            redirect.addFlashAttribute("message", "Your password has been changed.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + page;
    }
}
