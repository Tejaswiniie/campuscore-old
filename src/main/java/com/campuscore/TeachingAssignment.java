package com.campuscore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One class a faculty member teaches: a subject taught to one section. */
@Entity
@Table(name = "teaching_assignment", uniqueConstraints = {
    @UniqueConstraint(name = "uk_teaching_faculty_subject_section",
                      columnNames = {"faculty_id", "subject_id", "section_id"})})
public class TeachingAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }

    public String getLabel() { return subject.getName() + " | " + section.getLabel(); }
}
