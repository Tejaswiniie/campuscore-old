package com.campuscore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idea Hub rules: a student posts an idea (Pending), a faculty member approves or rejects it,
 * and students join approved ideas. The final team is decided in person with the mentor.
 */
@Service
public class IdeaService {
    private final IdeaRepository ideas;
    private final IdeaMemberRepository members;
    private final NameLookup names;

    public IdeaService(IdeaRepository ideas, IdeaMemberRepository members, NameLookup names) {
        this.ideas = ideas;
        this.members = members;
        this.names = names;
    }

    @Transactional
    public Idea post(String studentRoll, String title, String description) {
        String t = clean(title);
        String d = clean(description);
        if (t.isEmpty()) throw new IllegalArgumentException("Enter a title for your idea.");
        if (t.length() > 150) throw new IllegalArgumentException("The title is too long (150 characters at most).");
        if (d.isEmpty()) throw new IllegalArgumentException("Describe your idea.");
        if (d.length() > 2000) throw new IllegalArgumentException("The description is too long (2000 characters at most).");

        Idea i = new Idea();
        i.setStudentRoll(studentRoll);
        i.setAuthorName(names.student(studentRoll));
        i.setTitle(t);
        i.setDescription(d);
        i.setStatus("Pending");
        i.setCreatedAt(LocalDateTime.now());
        Idea saved = ideas.save(i);
        addMember(saved.getId(), studentRoll);   // the author is the first interested student
        return saved;
    }

    @Transactional
    public void approve(Long ideaId, String facultyId) {
        Idea i = find(ideaId);
        if (!"Pending".equals(i.getStatus())) {
            throw new IllegalArgumentException("This idea has already been reviewed.");
        }
        i.setStatus("Approved");
        i.setReviewedBy(facultyId);
        i.setMentorId(facultyId);                       // the approving faculty member becomes the mentor
        i.setMentorName(names.faculty(facultyId));
        ideas.save(i);
    }

    @Transactional
    public void reject(Long ideaId, String facultyId, String reason) {
        Idea i = find(ideaId);
        if (!"Pending".equals(i.getStatus())) {
            throw new IllegalArgumentException("This idea has already been reviewed.");
        }
        String r = clean(reason);
        if (r.length() > 500) throw new IllegalArgumentException("The reason is too long (500 characters at most).");
        i.setStatus("Rejected");
        i.setReason(r.isEmpty() ? null : r);
        i.setReviewedBy(facultyId);
        ideas.save(i);
    }

    @Transactional
    public void join(Long ideaId, String studentRoll) {
        Idea i = find(ideaId);
        if (!"Approved".equals(i.getStatus())) {
            throw new IllegalArgumentException("Only approved ideas can be joined.");
        }
        if (members.existsByIdeaIdAndStudentRoll(ideaId, studentRoll)) {
            throw new IllegalArgumentException("You have already joined this idea.");
        }
        addMember(ideaId, studentRoll);
    }

    public List<IdeaCard> approvedCards(String studentRoll) {
        List<IdeaCard> out = new ArrayList<>();
        for (Idea i : ideas.findByStatusOrderByCreatedAtDesc("Approved")) {
            List<IdeaMember> m = members.findByIdeaIdOrderByIdAsc(i.getId());
            boolean joined = m.stream().anyMatch(x -> x.getStudentRoll().equals(studentRoll));
            out.add(new IdeaCard(i, m, joined));
        }
        return out;
    }

    /** Approved ideas the student has joined, so they can see each idea's faculty mentor. */
    public List<Idea> joinedApproved(String studentRoll) {
        List<Long> ids = new ArrayList<>();
        for (IdeaMember m : members.findByStudentRoll(studentRoll)) ids.add(m.getIdeaId());
        List<Idea> out = new ArrayList<>();
        if (ids.isEmpty()) return out;
        for (Idea i : ideas.findAllById(ids)) {
            if ("Approved".equals(i.getStatus())) out.add(i);
        }
        out.sort(Comparator.comparing(Idea::getCreatedAt).reversed());
        return out;
    }

    public List<Idea> myIdeas(String studentRoll) {
        return ideas.findByStudentRollOrderByCreatedAtDesc(studentRoll);
    }

    public List<Idea> pending() {
        return ideas.findByStatusOrderByCreatedAtDesc("Pending");
    }

    private Idea find(Long id) {
        return ideas.findById(id).orElseThrow(() -> new IllegalArgumentException("Idea not found."));
    }

    private void addMember(Long ideaId, String studentRoll) {
        IdeaMember m = new IdeaMember();
        m.setIdeaId(ideaId);
        m.setStudentRoll(studentRoll);
        m.setStudentName(names.student(studentRoll));
        m.setJoinedAt(LocalDateTime.now());
        members.save(m);
    }

    private static String clean(String x) {
        return x == null ? "" : x.trim();
    }
}
