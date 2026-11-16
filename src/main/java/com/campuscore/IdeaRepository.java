package com.campuscore;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaRepository extends JpaRepository<Idea, Long> {
    List<Idea> findByStatusOrderByCreatedAtDesc(String status);
    List<Idea> findByStudentRollOrderByCreatedAtDesc(String studentRoll);
}
