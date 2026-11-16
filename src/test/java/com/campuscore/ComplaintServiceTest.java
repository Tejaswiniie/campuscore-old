package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ComplaintServiceTest {

    @Autowired ComplaintService service;

    @Test void newComplaintStartsAsSubmitted() {
        Complaint c = service.submit("CS1", "Hostel", "Water", "No water on the 2nd floor");
        assertEquals("Submitted", c.getStatus());
        assertNull(c.getResponse());
    }

    @Test void studentSeesOnlyTheirOwnComplaints() {
        service.submit("CS1", "Hostel", "Water", "No water");
        service.submit("CS2", "Academics", "Timetable", "Clash on Monday");
        List<Complaint> mine = service.forStudent("CS1");
        assertEquals(1, mine.size());
        assertEquals("CS1", mine.get(0).getStudentRoll());
    }

    @Test void unknownCategoryIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> service.submit("CS1", "Weather", "x", "y"));
    }

    @Test void blankSubjectIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> service.submit("CS1", "Other", "  ", "text"));
    }

    @Test void adminCanSetStatusAndReply() {
        Complaint c = service.submit("CS1", "Facilities", "Fan", "Fan is broken");
        service.update(c.getId(), "Resolved", "Fixed on Friday");
        Complaint saved = service.forStudent("CS1").get(0);
        assertEquals("Resolved", saved.getStatus());
        assertEquals("Fixed on Friday", saved.getResponse());
    }

    @Test void invalidStatusIsRejected() {
        Complaint c = service.submit("CS1", "Other", "x", "y");
        assertThrows(IllegalArgumentException.class, () -> service.update(c.getId(), "Done", ""));
    }
}
