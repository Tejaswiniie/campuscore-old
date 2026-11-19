package com.campuscore;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByDepartmentIdOrderByYearOfStudyAscNameAsc(Long departmentId);

    @Query("select s from Section s order by s.department.code, s.yearOfStudy, s.name")
    List<Section> findAllOrdered();

    List<Section> findByDepartmentIdAndYearOfStudyOrderByNameAsc(Long departmentId, int yearOfStudy);

    long countByDepartmentId(Long departmentId);

    boolean existsByDepartmentId(Long departmentId);

    boolean existsByDepartmentIdAndYearOfStudyAndNameIgnoreCase(Long departmentId, int yearOfStudy, String name);

    Optional<Section> findByDepartmentIdAndYearOfStudyAndNameIgnoreCase(Long departmentId, int yearOfStudy, String name);
}
