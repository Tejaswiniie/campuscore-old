package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FacultyClassServiceTest {

    @Autowired FacultyClassService service;
    @Autowired Fixtures fx;

    @Test void facultySeesOnlyTheirOwnClasses() {
        Department d = fx.dept("FC1");
        Section sec = fx.section("FC1", 2, "A");
        Faculty mine = fx.faculty("FC1F1", d);
        Faculty other = fx.faculty("FC1F2", d);
        fx.teach(mine, fx.subject("FC1S1", d), sec);
        fx.teach(other, fx.subject("FC1S2", d), sec);
        fx.student("FC1ST1", sec);
        List<ClassRow> rows = service.classesOf("FC1F1");
        assertEquals(1, rows.size());
        assertEquals("FC1S1", rows.get(0).getTeaching().getSubject().getCode());
        assertEquals(1, rows.get(0).getStudentCount());
    }

    @Test void facultySeesStudentsOfTheirOwnClass() {
        Department d = fx.dept("FC2");
        Section sec = fx.section("FC2", 2, "A");
        Faculty f = fx.faculty("FC2F1", d);
        TeachingAssignment t = fx.teach(f, fx.subject("FC2S1", d), sec);
        fx.student("FC2ST1", sec);
        fx.student("FC2ST2", sec);
        assertEquals(2, service.studentsOf("FC2F1", t.getId(), null).size());
        assertEquals(1, service.studentsOf("FC2F1", t.getId(), "FC2ST2").size());
    }

    @Test void facultyCannotOpenAnotherFacultyMembersClass() {
        Department d = fx.dept("FC3");
        Section sec = fx.section("FC3", 2, "A");
        Faculty owner = fx.faculty("FC3F1", d);
        fx.faculty("FC3F2", d);
        TeachingAssignment t = fx.teach(owner, fx.subject("FC3S1", d), sec);
        assertThrows(AccessDeniedException.class, () -> service.studentsOf("FC3F2", t.getId(), null));
        assertThrows(AccessDeniedException.class, () -> service.requireClass("FC3F2", t.getId()));
    }

    @Test void unknownClassIsRefused() {
        assertThrows(AccessDeniedException.class, () -> service.requireClass("ANY", 987654L));
        assertThrows(AccessDeniedException.class, () -> service.requireClass("ANY", null));
    }
}
