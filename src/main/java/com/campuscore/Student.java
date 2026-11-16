package com.campuscore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

/** Official student record. Department and year come from the student's section. */
@Entity
public class Student {
    @Id
    private String rollNo;
    @Column(nullable = false)
    private String name;
    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;
    private LocalDate dob;
    private double attendance;
    private double cgpa;

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
    public double getAttendance() { return attendance; }
    public void setAttendance(double attendance) { this.attendance = attendance; }
    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public Department getDepartment() { return section == null ? null : section.getDepartment(); }
    public String getDepartmentName() { return section == null ? null : section.getDepartment().getName(); }
    public String getDepartmentCode() { return section == null ? null : section.getDepartment().getCode(); }
    public Integer getYearOfStudy() { return section == null ? null : section.getYearOfStudy(); }
    public String getYearLabel() { return section == null ? null : section.getYearLabel(); }
    public String getSectionName() { return section == null ? null : section.getName(); }
}
