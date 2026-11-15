package com.campuscore;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One login row per person. The username is the roll number, faculty ID or "admin". */
@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    private String username;
    private String passwordHash;
    private String role;   // ADMIN, STUDENT or FACULTY

    public AppUser() {}
    public AppUser(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
