package com.campuscore;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Assignments (classwork). A faculty member can create, edit and delete only their own assignments,
 * and only for classes they teach. Students see the assignments of their own section.
 * An assignment can carry one attachment, which only its faculty member and the students of its section can download.
 */
@Service
public class AssignmentService {

    /** A file ready to be sent to the browser. */
    public record Download(String fileName, String contentType, long size, Path path) {}

    private final AssignmentRepository assignments;
    private final StudentRepository students;
    private final FacultyClassService classes;
    private final AttachmentStorage storage;

    public AssignmentService(AssignmentRepository assignments, StudentRepository students,
                             FacultyClassService classes, AttachmentStorage storage) {
        this.assignments = assignments;
        this.students = students;
        this.classes = classes;
        this.storage = storage;
    }

    /** Creates an assignment without an attachment. */
    @Transactional
    public Assignment create(String facultyId, Long teachingId, String title, String description, LocalDate dueDate) {
        return create(facultyId, teachingId, title, description, dueDate, null);
    }

    /** Creates an assignment. When a file is chosen it is validated and stored with the assignment. */
    @Transactional
    public Assignment create(String facultyId, Long teachingId, String title, String description,
                             LocalDate dueDate, MultipartFile file) {
        TeachingAssignment cls = classes.requireClass(facultyId, teachingId);
        String t = clean(title);
        String d = clean(description);
        check(t, d, dueDate);
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("The due date cannot be in the past.");
        }
        Assignment a = new Assignment();
        a.setFaculty(cls.getFaculty());
        a.setSubject(cls.getSubject());
        a.setSection(cls.getSection());
        a.setTitle(t);
        a.setDescription(d);
        a.setDueDate(dueDate);
        a.setCreatedAt(LocalDateTime.now());
        AttachmentStorage.StoredFile stored = isChosen(file) ? storage.store(file) : null;
        if (stored != null) setAttachment(a, stored);
        try {
            return assignments.saveAndFlush(a);
        } catch (RuntimeException e) {
            if (stored != null) storage.delete(stored.storedName());
            throw e;
        }
    }

    /** Updates the details of an assignment and keeps its attachment as it is. */
    @Transactional
    public Assignment update(String facultyId, Long assignmentId, Long teachingId,
                             String title, String description, LocalDate dueDate) {
        return update(facultyId, assignmentId, teachingId, title, description, dueDate, null, false);
    }

    /**
     * Updates an assignment. A chosen file replaces the current attachment; otherwise the attachment is
     * removed only when removeAttachment is true. The old file is deleted only after the change is saved.
     */
    @Transactional
    public Assignment update(String facultyId, Long assignmentId, Long teachingId, String title,
                             String description, LocalDate dueDate, MultipartFile file, boolean removeAttachment) {
        Assignment a = owned(facultyId, assignmentId);
        TeachingAssignment cls = classes.requireClass(facultyId, teachingId);
        String t = clean(title);
        String d = clean(description);
        check(t, d, dueDate);
        String oldStored = a.getAttachmentStoredName();
        AttachmentStorage.StoredFile stored = isChosen(file) ? storage.store(file) : null;
        a.setSubject(cls.getSubject());
        a.setSection(cls.getSection());
        a.setTitle(t);
        a.setDescription(d);
        a.setDueDate(dueDate);
        if (stored != null) {
            setAttachment(a, stored);
        } else if (removeAttachment) {
            clearAttachment(a);
        }
        Assignment saved;
        try {
            saved = assignments.saveAndFlush(a);
        } catch (RuntimeException e) {
            if (stored != null) storage.delete(stored.storedName());
            throw e;
        }
        if (oldStored != null && !oldStored.equals(saved.getAttachmentStoredName())) {
            deleteIfUnused(oldStored);
        }
        return saved;
    }

    @Transactional
    public void delete(String facultyId, Long assignmentId) {
        Assignment a = owned(facultyId, assignmentId);
        String stored = a.getAttachmentStoredName();
        assignments.delete(a);
        assignments.flush();
        deleteIfUnused(stored);
    }

    /** Deletes every assignment of a faculty member together with their attached files. */
    @Transactional
    public void deleteAllOf(String facultyId) {
        removeAll(assignments.findByFacultyFacultyIdOrderByDueDateAscIdAsc(facultyId));
    }

    /** Deletes the assignments a faculty member posted for one subject and section, with their files. */
    @Transactional
    public void deleteForClass(String facultyId, Long subjectId, Long sectionId) {
        removeAll(assignments.findByFacultyFacultyIdAndSubjectIdAndSectionId(facultyId, subjectId, sectionId));
    }

    private void removeAll(List<Assignment> list) {
        List<String> files = new ArrayList<>();
        for (Assignment a : list) {
            if (a.getAttachmentStoredName() != null) files.add(a.getAttachmentStoredName());
        }
        assignments.deleteAll(list);
        assignments.flush();
        for (String f : files) deleteIfUnused(f);
    }

    /** Returns the assignment only if this faculty member created it, otherwise refuses. */
    public Assignment owned(String facultyId, Long assignmentId) {
        Assignment a = assignmentId == null ? null : assignments.findById(assignmentId).orElse(null);
        if (a == null) throw new IllegalArgumentException("Assignment not found.");
        if (!a.getFaculty().getFacultyId().equals(facultyId)) {
            throw new AccessDeniedException("This assignment belongs to another faculty member.");
        }
        return a;
    }

    public List<Assignment> forFaculty(String facultyId) {
        return assignments.findByFacultyFacultyIdOrderByDueDateAscIdAsc(facultyId);
    }

    public long countPostedBy(String facultyId) {
        return assignments.countByFacultyFacultyId(facultyId);
    }

    /** Assignments for the student's own year and section. */
    public List<Assignment> visibleToStudent(String rollNo) {
        Student s = students.findById(rollNo).orElse(null);
        if (s == null || s.getSection() == null) return List.of();
        return assignments.findBySectionIdOrderByDueDateAscIdAsc(s.getSection().getId());
    }

    // ---- downloads ----

    /** The attachment of an assignment, for the faculty member who owns it. */
    public Download attachmentForFaculty(String facultyId, Long assignmentId) {
        return toDownload(owned(facultyId, assignmentId));
    }

    /**
     * The attachment of an assignment, for a student of the same section only. A missing assignment and an
     * assignment of another section give the same refusal, so IDs cannot be probed.
     */
    public Download attachmentForStudent(String rollNo, Long assignmentId) {
        Student s = rollNo == null ? null : students.findById(rollNo).orElse(null);
        Assignment a = assignmentId == null ? null : assignments.findById(assignmentId).orElse(null);
        if (s == null || s.getSection() == null || a == null
                || !a.getSection().getId().equals(s.getSection().getId())) {
            throw new AccessDeniedException("You cannot download this file.");
        }
        return toDownload(a);
    }

    private Download toDownload(Assignment a) {
        if (!a.isAttached()) throw new IllegalArgumentException("This assignment has no attachment.");
        Path path = storage.pathOf(a.getAttachmentStoredName());
        long size = a.getAttachmentSize() == null ? 0 : a.getAttachmentSize();
        return new Download(a.getAttachmentOriginalName(), a.getAttachmentContentType(), size, path);
    }

    // ---- helpers ----

    private static boolean isChosen(MultipartFile file) {
        return file != null && file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank();
    }

    private static void setAttachment(Assignment a, AttachmentStorage.StoredFile f) {
        a.setAttachmentOriginalName(f.originalName());
        a.setAttachmentStoredName(f.storedName());
        a.setAttachmentContentType(f.contentType());
        a.setAttachmentSize(f.size());
        a.setAttachmentUploadedAt(LocalDateTime.now());
    }

    private static void clearAttachment(Assignment a) {
        a.setAttachmentOriginalName(null);
        a.setAttachmentStoredName(null);
        a.setAttachmentContentType(null);
        a.setAttachmentSize(null);
        a.setAttachmentUploadedAt(null);
    }

    /** Deletes a stored file, but never one that another assignment still points to. */
    private void deleteIfUnused(String storedName) {
        if (storedName != null && assignments.countByAttachmentStoredName(storedName) == 0) {
            storage.delete(storedName);
        }
    }

    private static void check(String title, String description, LocalDate dueDate) {
        if (title.isEmpty()) throw new IllegalArgumentException("Enter a title.");
        if (title.length() > 150) throw new IllegalArgumentException("The title is too long (150 characters at most).");
        if (description.isEmpty()) throw new IllegalArgumentException("Enter a description.");
        if (description.length() > 2000) throw new IllegalArgumentException("The description is too long (2000 characters at most).");
        if (dueDate == null) throw new IllegalArgumentException("Choose a due date.");
    }

    private static String clean(String x) {
        return x == null ? "" : x.trim();
    }
}
