package com.campuscore;

/** A department with its section and student counts, calculated from the database. */
public class DepartmentRow {
    private final Department department;
    private final long sectionCount;
    private final long studentCount;

    public DepartmentRow(Department department, long sectionCount, long studentCount) {
        this.department = department;
        this.sectionCount = sectionCount;
        this.studentCount = studentCount;
    }
    public Department getDepartment() { return department; }
    public long getSectionCount() { return sectionCount; }
    public long getStudentCount() { return studentCount; }
}
