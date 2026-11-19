package com.campuscore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

/** Attachments on assignments: upload, replace, remove, ownership, section access and clean-up. */
@SpringBootTest
@Transactional
class AssignmentAttachmentTest {

    @Autowired AssignmentService service;
    @Autowired AttachmentStorage storage;
    @Autowired AdminService admin;
    @Autowired Fixtures fx;

    private final LocalDate due = LocalDate.now().plusDays(5);

    private TeachingAssignment classFor(String tag, String facultyId) {
        Department d = fx.dept(tag);
        Section sec = fx.section(tag, 2, "A");
        Faculty f = fx.faculty(facultyId, d);
        return fx.teach(f, fx.subject(tag + "S", d), sec);
    }

    private Assignment post(TeachingAssignment t, String facultyId, String title, String fileName) {
        return service.create(facultyId, t.getId(), title, "Description", due,
            fileName == null ? null : TestFiles.file(fileName, TestFiles.pdf()));
    }

    @Test void facultyCreatesAnAssignmentWithAnAttachment() {
        TeachingAssignment t = classFor("AT1", "AT1F1");
        Assignment a = post(t, "AT1F1", "Lab 1", "Unit 1 Notes.pdf");
        assertTrue(a.isAttached());
        assertEquals("Unit 1 Notes.pdf", a.getAttachmentOriginalName());
        assertEquals("application/pdf", a.getAttachmentContentType());
        assertEquals((long) TestFiles.pdf().length, a.getAttachmentSize().longValue());
        assertTrue(storage.exists(a.getAttachmentStoredName()));
        assertEquals("Lab 1", a.getTitle());
    }

    @Test void anAssignmentWithoutAFileStillWorks() {
        TeachingAssignment t = classFor("AT2", "AT2F1");
        Assignment a = post(t, "AT2F1", "Plain", null);
        assertFalse(a.isAttached());
        assertEquals(1, service.forFaculty("AT2F1").size());
    }

    @Test void unsupportedEmptyAndOversizedFilesCreateNoAssignment() {
        TeachingAssignment t = classFor("AT3", "AT3F1");
        assertThrows(IllegalArgumentException.class, () -> post(t, "AT3F1", "Bad type", "tool.exe"));
        assertThrows(IllegalArgumentException.class, () ->
            service.create("AT3F1", t.getId(), "Empty", "D", due, TestFiles.file("empty.pdf", new byte[0])));
        byte[] big = new byte[(int) storage.maxBytes() + 1];
        assertThrows(IllegalArgumentException.class, () ->
            service.create("AT3F1", t.getId(), "Big", "D", due, TestFiles.file("big.pdf", big)));
        assertEquals(0, service.forFaculty("AT3F1").size());
    }

    @Test void replacingTheAttachmentDeletesOnlyTheOldFile() {
        TeachingAssignment t = classFor("AT4", "AT4F1");
        Assignment a = post(t, "AT4F1", "First", "old.pdf");
        Assignment other = post(t, "AT4F1", "Second", "other.pdf");
        String oldStored = a.getAttachmentStoredName();
        String otherStored = other.getAttachmentStoredName();

        Assignment updated = service.update("AT4F1", a.getId(), t.getId(), "First", "Description", due,
            TestFiles.file("new.pdf", TestFiles.pdf()), false);

        assertEquals("new.pdf", updated.getAttachmentOriginalName());
        assertNotEquals(oldStored, updated.getAttachmentStoredName());
        assertFalse(storage.exists(oldStored));
        assertTrue(storage.exists(updated.getAttachmentStoredName()));
        assertTrue(storage.exists(otherStored));           // another assignment's file is never touched
        assertEquals("First", updated.getTitle());          // the details stay as they were
    }

    @Test void removingTheAttachmentClearsItAndKeepsTheDetails() {
        TeachingAssignment t = classFor("AT5", "AT5F1");
        Assignment a = post(t, "AT5F1", "Keep me", "gone.pdf");
        String stored = a.getAttachmentStoredName();
        Assignment updated = service.update("AT5F1", a.getId(), t.getId(), "Keep me", "Description", due, null, true);
        assertFalse(updated.isAttached());
        assertNull(updated.getAttachmentOriginalName());
        assertFalse(storage.exists(stored));
        assertEquals("Keep me", updated.getTitle());
        assertEquals("Description", updated.getDescription());
    }

    @Test void editingWithoutAFileKeepsTheAttachment() {
        TeachingAssignment t = classFor("AT6", "AT6F1");
        Assignment a = post(t, "AT6F1", "Stay", "stay.pdf");
        String stored = a.getAttachmentStoredName();
        service.update("AT6F1", a.getId(), t.getId(), "Renamed", "Description", due);
        Assignment again = service.update("AT6F1", a.getId(), t.getId(), "Renamed twice", "Description", due, null, false);
        assertTrue(again.isAttached());
        assertEquals(stored, again.getAttachmentStoredName());
        assertTrue(storage.exists(stored));
    }

    @Test void anotherFacultyMemberCannotReplaceOrRemoveTheAttachment() {
        TeachingAssignment t = classFor("AT7", "AT7F1");
        fx.faculty("AT7F2", fx.dept("AT7"));
        Assignment a = post(t, "AT7F1", "Mine", "mine.pdf");
        assertThrows(AccessDeniedException.class, () -> service.update("AT7F2", a.getId(), t.getId(), "Mine", "D", due,
            TestFiles.file("evil.pdf", TestFiles.pdf()), false));
        assertThrows(AccessDeniedException.class, () -> service.update("AT7F2", a.getId(), t.getId(), "Mine", "D", due, null, true));
        assertThrows(AccessDeniedException.class, () -> service.attachmentForFaculty("AT7F2", a.getId()));
        assertTrue(storage.exists(a.getAttachmentStoredName()));
    }

    @Test void deletingAnAssignmentDeletesItsFileButNotOthers() {
        TeachingAssignment t = classFor("AT8", "AT8F1");
        Assignment a = post(t, "AT8F1", "Delete me", "a.pdf");
        Assignment b = post(t, "AT8F1", "Keep me", "b.pdf");
        service.delete("AT8F1", a.getId());
        assertFalse(storage.exists(a.getAttachmentStoredName()));
        assertTrue(storage.exists(b.getAttachmentStoredName()));
    }

    @Test void studentOfTheSameSectionDownloadsWithTheOriginalName() {
        TeachingAssignment t = classFor("AT9", "AT9F1");
        fx.student("AT9S1", t.getSection());
        Assignment a = post(t, "AT9F1", "Notes", "Unit 1 Notes.pdf");
        AssignmentService.Download d = service.attachmentForStudent("AT9S1", a.getId());
        assertEquals("Unit 1 Notes.pdf", d.fileName());
        assertEquals("application/pdf", d.contentType());
        assertTrue(d.path().toFile().exists());
    }

    @Test void studentOfAnotherSectionOrAnUnknownIdIsRefused() {
        TeachingAssignment t = classFor("AT10", "AT10F1");
        Section other = fx.section("AT10", 2, "B");
        fx.student("AT10S1", other);
        Assignment a = post(t, "AT10F1", "Notes", "n.pdf");
        assertThrows(AccessDeniedException.class, () -> service.attachmentForStudent("AT10S1", a.getId()));
        assertThrows(AccessDeniedException.class, () -> service.attachmentForStudent("AT10S1", 987654321L));
        assertThrows(AccessDeniedException.class, () -> service.attachmentForStudent("NOBODY", a.getId()));
    }

    @Test void ownerDownloadsAndAMissingAttachmentIsReported() {
        TeachingAssignment t = classFor("AT11", "AT11F1");
        fx.student("AT11S1", t.getSection());
        Assignment withFile = post(t, "AT11F1", "With", "w.pdf");
        Assignment without = post(t, "AT11F1", "Without", null);
        assertEquals("w.pdf", service.attachmentForFaculty("AT11F1", withFile.getId()).fileName());
        assertThrows(IllegalArgumentException.class, () -> service.attachmentForFaculty("AT11F1", without.getId()));
        assertThrows(IllegalArgumentException.class, () -> service.attachmentForStudent("AT11S1", without.getId()));
    }

    @Test void deletingAFacultyMemberAlsoDeletesTheirAttachmentFiles() {
        TeachingAssignment t = classFor("AT12", "AT12F1");
        Assignment a = post(t, "AT12F1", "Soon gone", "x.pdf");
        String stored = a.getAttachmentStoredName();
        admin.deleteFaculty("AT12F1");
        assertFalse(storage.exists(stored));
    }
}
