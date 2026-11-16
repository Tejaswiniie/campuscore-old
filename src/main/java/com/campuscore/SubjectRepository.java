package com.campuscore;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findAllByOrderByCodeAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByDepartmentId(Long departmentId);
}
