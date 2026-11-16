package com.campuscore;

/** Fields of the admin faculty form. */
public class FacultyForm {
    private String facultyId;
    private String name;
    private Long departmentId;
    private String designation;

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
