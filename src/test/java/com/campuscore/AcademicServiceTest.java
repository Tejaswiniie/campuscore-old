package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AcademicServiceTest {

    @Autowired AcademicService academic;
    @Autowired Fixtures fx;

    private DepartmentRow row(Department d) {
        for (DepartmentRow r : academic.departmentRows()) {
            if (r.getDepartment().getId().equals(d.getId())) return r;
        }
        throw new AssertionError("department not listed");
    }

    @Test void departmentCountsAreCalculatedFromTheDatabase() {
        Department d = fx.dept("AC1");
        Section a = fx.section("AC1", 2, "A");
        Section b = fx.section("AC1", 3, "A");
        fx.student("AC1S1", a);
        fx.student("AC1S2", a);
        fx.student("AC1S3", b);
        DepartmentRow r = row(d);
        assertEquals(2, r.getSectionCount());
        assertEquals(3, r.getStudentCount());
    }

    @Test void countsChangeWhenStudentsAreAdded() {
        Department d = fx.dept("AC2");
        Section a = fx.section("AC2", 1, "A");
        assertEquals(0, row(d).getStudentCount());
        fx.student("AC2S1", a);
        assertEquals(1, row(d).getStudentCount());
    }

    @Test void emptySectionsAreStillListedWithZeroStudents() {
        Department d = fx.dept("AC3");
        Section full = fx.section("AC3", 2, "A");
        Section empty = fx.section("AC3", 2, "B");
        fx.student("AC3S1", full);
        List<SectionRow> rows = academic.sectionRows(d.getId());
        assertEquals(2, rows.size());
        for (SectionRow r : rows) {
            if (r.getSection().getId().equals(empty.getId())) assertEquals(0, r.getStudentCount());
            else assertEquals(1, r.getStudentCount());
        }
    }

    @Test void sectionListsOnlyItsOwnStudents() {
        Section a = fx.section("AC4", 2, "A");
        Section b = fx.section("AC4", 2, "B");
        fx.student("AC4S1", a);
        fx.student("AC4S2", b);
        List<Student> inA = academic.studentsOfSection(a.getId(), null);
        assertEquals(1, inA.size());
        assertEquals("AC4S1", inA.get(0).getRollNo());
    }

    @Test void studentsInASectionCanBeSearchedByIdOrName() {
        Section a = fx.section("AC5", 2, "A");
        fx.student("AC5S1", a);
        fx.student("AC5S2", a);
        assertEquals(1, academic.studentsOfSection(a.getId(), "ac5s2").size());
        assertEquals(2, academic.studentsOfSection(a.getId(), "Student").size());
        assertTrue(academic.studentsOfSection(a.getId(), "zzz").isEmpty());
    }

    @Test void duplicateSectionIsRejected() {
        Department d = fx.dept("AC6");
        academic.createSection(d.getId(), 2, "A");
        assertThrows(IllegalArgumentException.class, () -> academic.createSection(d.getId(), 2, "a"));
    }

    @Test void sameSectionNameIsAllowedInAnotherYearOrDepartment() {
        Department d1 = fx.dept("AC7");
        Department d2 = fx.dept("AC8");
        academic.createSection(d1.getId(), 2, "A");
        academic.createSection(d1.getId(), 3, "A");
        academic.createSection(d2.getId(), 2, "A");
    }

    @Test void sectionYearMustBeValid() {
        Department d = fx.dept("AC9");
        assertThrows(IllegalArgumentException.class, () -> academic.createSection(d.getId(), 0, "A"));
        assertThrows(IllegalArgumentException.class, () -> academic.createSection(d.getId(), null, "A"));
    }

    @Test void duplicateDepartmentCodeIsRejected() {
        academic.createDepartment("DUPD", "Duplicate Dept");
        assertThrows(IllegalArgumentException.class, () -> academic.createDepartment("dupd", "Another name"));
    }

    @Test void sectionWithStudentsCannotBeDeleted() {
        Section a = fx.section("AC10", 2, "A");
        fx.student("AC10S1", a);
        assertThrows(IllegalArgumentException.class, () -> academic.deleteSection(a.getId()));
    }

    @Test void emptySectionCanBeDeleted() {
        Section a = fx.section("AC11", 2, "A");
        academic.deleteSection(a.getId());
        assertThrows(IllegalArgumentException.class, () -> academic.section(a.getId()));
    }

    @Test void departmentWithSectionsCannotBeDeleted() {
        Department d = fx.dept("AC12");
        fx.section("AC12", 1, "A");
        assertThrows(IllegalArgumentException.class, () -> academic.deleteDepartment(d.getId()));
    }

    @Test void teacherCannotBeAssignedToASubjectOfAnotherDepartment() {
        Department d1 = fx.dept("AC13");
        Department d2 = fx.dept("AC14");
        Section sec = fx.section("AC13", 2, "A");
        Subject sub = fx.subject("AC14SUB", d2);
        Faculty f = fx.faculty("AC13F", d1);
        assertThrows(IllegalArgumentException.class, () -> fx.teach(f, sub, sec));
    }

    @Test void sameClassCannotBeAssignedTwice() {
        Department d = fx.dept("AC15");
        Section sec = fx.section("AC15", 2, "A");
        Subject sub = fx.subject("AC15SUB", d);
        Faculty f = fx.faculty("AC15F", d);
        fx.teach(f, sub, sec);
        assertThrows(IllegalArgumentException.class, () -> fx.teach(f, sub, sec));
    }
}
