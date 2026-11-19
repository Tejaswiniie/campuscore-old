package com.campuscore;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Fields of the admin student form. */
public class StudentForm {
    private String rollNo;
    private String name;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;
    private Long departmentId;
    private Integer yearOfStudy;
    private Long sectionId;
    private Double attendance;
    private Double cgpa;

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }
    public Double getAttendance() { return attendance; }
    public void setAttendance(Double attendance) { this.attendance = attendance; }
    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }
}
