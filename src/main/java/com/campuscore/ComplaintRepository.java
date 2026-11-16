package com.campuscore;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStudentRollOrderByCreatedAtDesc(String studentRoll);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    long countByStatusNot(String status);
    void deleteByStudentRoll(String studentRoll);
}
