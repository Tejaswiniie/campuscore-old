package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IdeaMentorTest {

    @Autowired IdeaService service;
    @Autowired Fixtures fx;

    @Test void approvingFacultyBecomesTheMentor() {
        Department d = fx.dept("IM1");
        fx.faculty("IM1F1", d);
        Idea i = service.post("IM1S1", "Idea", "Text");
        assertNull(i.getMentorId());
        service.approve(i.getId(), "IM1F1");
        Idea saved = service.approvedCards("IM1S1").get(0).getIdea();
        assertEquals("IM1F1", saved.getMentorId());
        assertEquals("Faculty IM1F1", saved.getMentorName());
    }

    @Test void rejectedIdeaHasNoMentor() {
        Idea i = service.post("IM2S1", "Idea", "Text");
        service.reject(i.getId(), "IM2F1", "Not feasible");
        assertNull(service.myIdeas("IM2S1").get(0).getMentorId());
    }

    @Test void studentSeesTheMentorOfIdeasTheyJoined() {
        Section sec = fx.section("IM3", 2, "A");
        fx.faculty("IM3F1", fx.dept("IM3"));
        fx.student("IM3S1", sec);
        fx.student("IM3S2", sec);
        Idea i = service.post("IM3S1", "Idea", "Text");
        service.approve(i.getId(), "IM3F1");
        assertTrue(service.joinedApproved("IM3S2").isEmpty());
        service.join(i.getId(), "IM3S2");
        List<Idea> joined = service.joinedApproved("IM3S2");
        assertEquals(1, joined.size());
        assertEquals("IM3F1", joined.get(0).getMentorId());
    }

    @Test void pendingIdeasAreNotListedAsMentored() {
        service.post("IM4S1", "Idea", "Text");
        assertTrue(service.joinedApproved("IM4S1").isEmpty());
    }

    @Test void duplicateMembershipIsRefused() {
        Idea i = service.post("IM5S1", "Idea", "Text");
        service.approve(i.getId(), "IM5F1");
        assertThrows(IllegalArgumentException.class, () -> service.join(i.getId(), "IM5S1"));
    }
}
