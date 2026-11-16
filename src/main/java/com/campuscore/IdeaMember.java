package com.campuscore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** A student who is interested in an idea. A student can appear only once per idea. */
@Entity
@Table(name = "idea_member", uniqueConstraints = {
    @UniqueConstraint(name = "uk_idea_member", columnNames = {"idea_id", "student_roll"})})
public class IdeaMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "idea_id", nullable = false)
    private Long ideaId;
    @Column(name = "student_roll", nullable = false)
    private String studentRoll;
    private String studentName;
    private LocalDateTime joinedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdeaId() { return ideaId; }
    public void setIdeaId(Long ideaId) { this.ideaId = ideaId; }
    public String getStudentRoll() { return studentRoll; }
    public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
