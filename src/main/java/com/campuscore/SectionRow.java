package com.campuscore;

/** A section with its student count. Sections with no students are included. */
public class SectionRow {
    private final Section section;
    private final long studentCount;

    public SectionRow(Section section, long studentCount) {
        this.section = section;
        this.studentCount = studentCount;
    }
    public Section getSection() { return section; }
    public long getStudentCount() { return studentCount; }
}
