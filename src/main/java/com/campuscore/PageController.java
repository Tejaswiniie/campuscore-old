package com.campuscore;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String root(Authentication auth) {
        return "redirect:" + RoleRedirect.target(auth.getAuthorities());
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin")
    public String admin(Model model, Authentication auth) {
        model.addAttribute("name", auth.getName());
        return "admin";
    }

    @GetMapping("/faculty")
    public String faculty(Model model, Authentication auth) {
        model.addAttribute("name", auth.getName());
        return "faculty";
    }

    @GetMapping("/student")
    public String student(Model model, Authentication auth) {
        double attendance = 68;   // sample data until the database is added
        double cgpa = 8.2;
        model.addAttribute("name", auth.getName());
        model.addAttribute("attendance", attendance);
        model.addAttribute("zone", AttendanceZone.of(attendance));
        model.addAttribute("cgpa", cgpa);
        model.addAttribute("cgpaPercent", cgpa * 10);
        return "student";
    }
}
