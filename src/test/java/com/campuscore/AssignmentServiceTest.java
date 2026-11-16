package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AssignmentServiceTest {

    @Autowired AssignmentService service;
    @Autowired Fixtures fx;

    private final LocalDate due = LocalDate.now().plusDays(5);

    @Test void facultyCreatesAnAssignmentForTheirOwnClass() {
        Department d = fx.dept("AG1");
        Section sec = fx.section("AG1", 2, "A");
        Faculty f = fx.faculty("AG1F1", d);
        Subject sub = fx.subject("AG1S1", d);
        TeachingAssignment t = fx.teach(f, sub, sec);
        Assignment a = service.create("AG1F1", t.getId(), "Lab 1", "Do the lab", due);
        assertEquals(sub.getId(), a.getSubject().getId());
        assertEquals(sec.getId(), a.getSection().getId());
        assertEquals(1, service.forFaculty("AG1F1").size());
    }

    @Test void facultyCannotCreateAnAssignmentForAClassTheyDoNotTeach() {
        Department d = fx.dept("AG2");
        Section sec = fx.section("AG2", 2, "A");
        Faculty owner = fx.faculty("AG2F1", d);
        fx.faculty("AG2F2", d);
        TeachingAssignment t = fx.teach(owner, fx.subject("AG2S1", d), sec);
        assertThrows(AccessDeniedException.class,
            () -> service.create("AG2F2", t.getId(), "Sneaky", "Not mine", due));
    }

    @Test void facultyCannotEditOrDeleteAnotherFacultyMembersAssignment() {
        Department d = fx.dept("AG3");
        Section sec = fx.section("AG3", 2, "A");
        Faculty owner = fx.faculty("AG3F1", d);
        Faculty other = fx.faculty("AG3F2", d);
        TeachingAssignment t1 = fx.teach(owner, fx.subject("AG3S1", d), sec);
        TeachingAssignment t2 = fx.teach(other, fx.subject("AG3S2", d), sec);
        Assignment a = service.create("AG3F1", t1.getId(), "Mine", "Text", due);
        assertThrows(AccessDeniedException.class,
            () -> service.update("AG3F2", a.getId(), t2.getId(), "Hijack", "Text", due));
        assertThrows(AccessDeniedException.class, () -> service.delete("AG3F2", a.getId()));
        assertEquals("Mine", service.owned("AG3F1", a.getId()).getTitle());
    }

    @Test void facultyCanEditAndDeleteTheirOwnAssignment() {
        Department d = fx.dept("AG4");
        Section sec = fx.section("AG4", 2, "A");
        Faculty f = fx.faculty("AG4F1", d);
        TeachingAssignment t = fx.teach(f, fx.subject("AG4S1", d), sec);
        Assignment a = service.create("AG4F1", t.getId(), "Old", "Text", due);
        service.update("AG4F1", a.getId(), t.getId(), "New", "New text", due.plusDays(1));
        assertEquals("New", service.owned("AG4F1", a.getId()).getTitle());
        service.delete("AG4F1", a.getId());
        assertTrue(service.forFaculty("AG4F1").isEmpty());
    }

    @Test void facultyCannotMoveAnAssignmentToAClassTheyDoNotTeach() {
        Department d = fx.dept("AG5");
        Section sec = fx.section("AG5", 2, "A");
        Faculty f1 = fx.faculty("AG5F1", d);
        Faculty f2 = fx.faculty("AG5F2", d);
        TeachingAssignment mine = fx.teach(f1, fx.subject("AG5S1", d), sec);
        TeachingAssignment theirs = fx.teach(f2, fx.subject("AG5S2", d), sec);
        Assignment a = service.create("AG5F1", mine.getId(), "Mine", "Text", due);
        assertThrows(AccessDeniedException.class,
            () -> service.update("AG5F1", a.getId(), theirs.getId(), "Mine", "Text", due));
    }

    @Test void studentsSeeOnlyAssignmentsOfTheirOwnYearAndSection() {
        Department d = fx.dept("AG6");
        Section a2 = fx.section("AG6", 2, "A");
        Section b2 = fx.section("AG6", 2, "B");
        Section a3 = fx.section("AG6", 3, "A");
        Faculty f = fx.faculty("AG6F1", d);
        Subject sub = fx.subject("AG6S1", d);
        TeachingAssignment ta = fx.teach(f, sub, a2);
        TeachingAssignment tb = fx.teach(f, sub, b2);
        TeachingAssignment tc = fx.teach(f, sub, a3);
        service.create("AG6F1", ta.getId(), "For 2A", "Text", due);
        service.create("AG6F1", tb.getId(), "For 2B", "Text", due);
        service.create("AG6F1", tc.getId(), "For 3A", "Text", due);
        fx.student("AG6ST1", a2);
        List<Assignment> seen = service.visibleToStudent("AG6ST1");
        assertEquals(1, seen.size());
        assertEquals("For 2A", seen.get(0).getTitle());
    }

    @Test void unknownStudentSeesNothing() {
        assertTrue(service.visibleToStudent("NOBODY").isEmpty());
    }

    @Test void invalidAssignmentsAreRejected() {
        Department d = fx.dept("AG7");
        Section sec = fx.section("AG7", 2, "A");
        Faculty f = fx.faculty("AG7F1", d);
        TeachingAssignment t = fx.teach(f, fx.subject("AG7S1", d), sec);
        assertThrows(IllegalArgumentException.class, () -> service.create("AG7F1", t.getId(), " ", "Text", due));
        assertThrows(IllegalArgumentException.class, () -> service.create("AG7F1", t.getId(), "T", " ", due));
        assertThrows(IllegalArgumentException.class, () -> service.create("AG7F1", t.getId(), "T", "Text", null));
        assertThrows(IllegalArgumentException.class,
            () -> service.create("AG7F1", t.getId(), "T", "Text", LocalDate.now().minusDays(1)));
    }
}
