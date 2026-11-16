package com.campuscore;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, String> {
    List<Student> findAllByOrderByRollNoAsc();

    List<Student> findBySectionIdOrderByRollNoAsc(Long sectionId);

    long countBySectionId(Long sectionId);

    boolean existsBySectionId(Long sectionId);

    @Query("select count(s) from Student s where s.section.department.id = :departmentId")
    long countByDepartment(@Param("departmentId") Long departmentId);

    @Query("select s from Student s where lower(s.rollNo) like lower(concat('%', :q, '%')) "
         + "or lower(s.name) like lower(concat('%', :q, '%')) order by s.rollNo")
    List<Student> search(@Param("q") String q);

    @Query("select s from Student s where s.section.id = :sectionId and "
         + "(lower(s.rollNo) like lower(concat('%', :q, '%')) or lower(s.name) like lower(concat('%', :q, '%'))) "
         + "order by s.rollNo")
    List<Student> searchInSection(@Param("sectionId") Long sectionId, @Param("q") String q);
}
