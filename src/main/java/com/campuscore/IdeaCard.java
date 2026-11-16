package com.campuscore;

import java.util.List;

/** An idea together with its interested students, for display. */
public class IdeaCard {
    private final Idea idea;
    private final List<IdeaMember> members;
    private final boolean joined;

    public IdeaCard(Idea idea, List<IdeaMember> members, boolean joined) {
        this.idea = idea;
        this.members = members;
        this.joined = joined;
    }
    public Idea getIdea() { return idea; }
    public List<IdeaMember> getMembers() { return members; }
    public boolean isJoined() { return joined; }
    public int getMemberCount() { return members.size(); }
}
