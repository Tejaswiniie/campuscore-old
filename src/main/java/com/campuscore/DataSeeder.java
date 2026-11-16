package com.campuscore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the admin account and the fixed department list at startup. Students and faculty are added by the admin.
 * The first admin password comes from app.admin.password (environment variable ADMIN_PASSWORD).
 * It is used only when the admin account does not exist yet.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {
    private final AppUserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;
    private final String adminPassword;

    public DataSeeder(AppUserRepository users, DepartmentRepository departments, PasswordEncoder encoder,
                      @Value("${app.admin.password:admin123}") String adminPassword) {
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!users.existsById("admin")) {
            users.save(new AppUser("admin", encoder.encode(adminPassword), "ADMIN"));
        }
        // The fixed list of departments. Missing ones are added; existing ones are never changed.
        String[][] fixed = {
            {"CSM", "Computer Science and Engineering (AI and ML)"},
            {"CSE", "Computer Science and Engineering"},
            {"ECE", "Electronics and Communication Engineering"},
            {"EEE", "Electrical and Electronics Engineering"},
            {"CSC", "Computer Science and Engineering (Cyber Security)"},
            {"CSD", "Computer Science and Engineering (Data Science)"},
            {"MECH", "Mechanical Engineering"}};
        for (String[] d : fixed) {
            if (!departments.existsByCodeIgnoreCase(d[0])) {
                Department dep = new Department();
                dep.setCode(d[0]);
                dep.setName(d[1]);
                departments.save(dep);
            }
        }
    }
}
