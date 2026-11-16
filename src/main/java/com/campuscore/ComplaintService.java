package com.campuscore;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintService {
    public static final List<String> CATEGORIES = List.of("Academics", "Facilities", "Hostel", "Other");
    public static final List<String> STATUSES = List.of("Submitted", "In Review", "Resolved");

    private final ComplaintRepository repo;

    public ComplaintService(ComplaintRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Complaint submit(String studentRoll, String category, String subject, String description) {
        String s = clean(subject);
        String d = clean(description);
        if (category == null || !CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Choose a category.");
        }
        if (s.isEmpty()) throw new IllegalArgumentException("Enter a subject.");
        if (s.length() > 150) throw new IllegalArgumentException("The subject is too long (150 characters at most).");
        if (d.isEmpty()) throw new IllegalArgumentException("Describe your complaint.");
        if (d.length() > 2000) throw new IllegalArgumentException("The description is too long (2000 characters at most).");

        Complaint c = new Complaint();
        c.setStudentRoll(studentRoll);
        c.setCategory(category);
        c.setSubject(s);
        c.setDescription(d);
        c.setStatus("Submitted");
        c.setCreatedAt(LocalDateTime.now());
        return repo.save(c);
    }

    public List<Complaint> forStudent(String studentRoll) {
        return repo.findByStudentRollOrderByCreatedAtDesc(studentRoll);
    }

    public long openCount() {
        return repo.countByStatusNot("Resolved");
    }

    public List<Complaint> all() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void update(Long id, String status, String response) {
        if (status == null || !STATUSES.contains(status)) {
            throw new IllegalArgumentException("Choose a valid status.");
        }
        Complaint c = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));
        String r = clean(response);
        if (r.length() > 1000) throw new IllegalArgumentException("The reply is too long (1000 characters at most).");
        c.setStatus(status);
        c.setResponse(r.isEmpty() ? null : r);
        repo.save(c);
    }

    private static String clean(String x) {
        return x == null ? "" : x.trim();
    }
}
