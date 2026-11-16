package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IdeaServiceTest {

    @Autowired IdeaService service;

    @Test void postedIdeaIsPendingAndAuthorIsInterested() {
        Idea i = service.post("CS1", "Smart bus tracker", "Track the college bus live");
        assertEquals("Pending", i.getStatus());
        service.approve(i.getId(), "FAC1");
        List<IdeaCard> cards = service.approvedCards("CS1");
        assertEquals(1, cards.size());
        assertEquals(1, cards.get(0).getMemberCount());
        assertTrue(cards.get(0).isJoined());
    }

    @Test void pendingIdeaCannotBeJoined() {
        Idea i = service.post("CS1", "Idea", "Text");
        assertThrows(IllegalArgumentException.class, () -> service.join(i.getId(), "CS2"));
    }

    @Test void studentCanJoinAnApprovedIdeaOnlyOnce() {
        Idea i = service.post("CS1", "Idea", "Text");
        service.approve(i.getId(), "FAC1");
        service.join(i.getId(), "CS2");
        assertThrows(IllegalArgumentException.class, () -> service.join(i.getId(), "CS2"));
        assertEquals(2, service.approvedCards("CS3").get(0).getMemberCount());
        assertFalse(service.approvedCards("CS3").get(0).isJoined());
    }

    @Test void rejectedIdeaKeepsTheReasonAndCannotBeJoined() {
        Idea i = service.post("CS1", "Idea", "Text");
        service.reject(i.getId(), "FAC1", "Too broad");
        Idea saved = service.myIdeas("CS1").get(0);
        assertEquals("Rejected", saved.getStatus());
        assertEquals("Too broad", saved.getReason());
        assertThrows(IllegalArgumentException.class, () -> service.join(i.getId(), "CS2"));
    }

    @Test void anIdeaCanBeReviewedOnlyOnce() {
        Idea i = service.post("CS1", "Idea", "Text");
        service.approve(i.getId(), "FAC1");
        assertThrows(IllegalArgumentException.class, () -> service.reject(i.getId(), "FAC2", "no"));
    }

    @Test void emptyTitleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.post("CS1", " ", "Text"));
    }
}
