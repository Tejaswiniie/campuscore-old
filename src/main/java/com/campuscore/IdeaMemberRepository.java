package com.campuscore;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaMemberRepository extends JpaRepository<IdeaMember, Long> {
    List<IdeaMember> findByIdeaIdOrderByIdAsc(Long ideaId);
    List<IdeaMember> findByStudentRoll(String studentRoll);
    boolean existsByIdeaIdAndStudentRoll(Long ideaId, String studentRoll);
    void deleteByStudentRoll(String studentRoll);
}
