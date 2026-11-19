package com.campuscore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A class section such as "CSE, year 2, section A". Unique per department, year and name. */
@Entity
@Table(name = "class_section", uniqueConstraints = {
    @UniqueConstraint(name = "uk_section_dept_year_name",
                      columnNames = {"department_id", "year_of_study", "name"})})
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    @Column(name = "year_of_study", nullable = false)
    private int yearOfStudy;
    @Column(name = "name", nullable = false, length = 20)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** Text shown in drop-down lists. */
    public String getLabel() {
        return department.getCode() + " | " + getYearLabel() + " | Section " + name;
    }

    /** "1st yr", "2nd yr", "3rd yr", "4th yr". */
    public String getYearLabel() {
        return yearLabel(yearOfStudy);
    }

    /** "1st Year", "2nd Year", "3rd Year", "4th Year" (used in drop-down lists). */
    public static String yearName(int year) {
        switch (year) {
            case 1: return "1st Year";
            case 2: return "2nd Year";
            case 3: return "3rd Year";
            default: return year + "th Year";
        }
    }

    public static String yearLabel(int year) {
        switch (year) {
            case 1: return "1st yr";
            case 2: return "2nd yr";
            case 3: return "3rd yr";
            default: return year + "th yr";
        }
    }
}
