package com.campuscore;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Fields of the faculty assignment form. */
public class AssignmentForm {
    private Long teachingId;
    private String title;
    private String description;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    public Long getTeachingId() { return teachingId; }
    public void setTeachingId(Long teachingId) { this.teachingId = teachingId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
