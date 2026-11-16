package com.campuscore;

/** A class a faculty member teaches, with the number of students in it. */
public class ClassRow {
    private final TeachingAssignment teaching;
    private final long studentCount;

    public ClassRow(TeachingAssignment teaching, long studentCount) {
        this.teaching = teaching;
        this.studentCount = studentCount;
    }
    public TeachingAssignment getTeaching() { return teaching; }
    public long getStudentCount() { return studentCount; }
}
