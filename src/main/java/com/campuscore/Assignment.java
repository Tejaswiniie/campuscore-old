package com.campuscore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Classwork posted by a faculty member for one subject and section. No submissions in this version. */
@Entity
@Table(name = "class_assignment")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;
    @Column(name = "title", nullable = false, length = 150)
    private String title;
    @Column(name = "description", nullable = false, length = 2000)
    private String description;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ---- attachment (at most one file per assignment) ----
    @Column(name = "attachment_original_name", length = 200)
    private String attachmentOriginalName;
    @Column(name = "attachment_stored_name", length = 80)
    private String attachmentStoredName;
    @Column(name = "attachment_content_type", length = 120)
    private String attachmentContentType;
    @Column(name = "attachment_size")
    private Long attachmentSize;
    @Column(name = "attachment_uploaded_at")
    private LocalDateTime attachmentUploadedAt;

    public String getAttachmentOriginalName() { return attachmentOriginalName; }
    public void setAttachmentOriginalName(String v) { this.attachmentOriginalName = v; }
    public String getAttachmentStoredName() { return attachmentStoredName; }
    public void setAttachmentStoredName(String v) { this.attachmentStoredName = v; }
    public String getAttachmentContentType() { return attachmentContentType; }
    public void setAttachmentContentType(String v) { this.attachmentContentType = v; }
    public Long getAttachmentSize() { return attachmentSize; }
    public void setAttachmentSize(Long v) { this.attachmentSize = v; }
    public LocalDateTime getAttachmentUploadedAt() { return attachmentUploadedAt; }
    public void setAttachmentUploadedAt(LocalDateTime v) { this.attachmentUploadedAt = v; }

    public boolean isAttached() {
        return attachmentStoredName != null && !attachmentStoredName.isBlank();
    }

    public String getAttachmentSizeLabel() {
        if (attachmentSize == null) return "";
        long b = attachmentSize;
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return (b / 1024) + " KB";
        return String.format(java.util.Locale.ROOT, "%.1f MB", b / (1024.0 * 1024.0));
    }

    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now());
    }
}
