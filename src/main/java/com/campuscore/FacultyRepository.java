package com.campuscore;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacultyRepository extends JpaRepository<Faculty, String> {
    List<Faculty> findAllByOrderByFacultyIdAsc();

    boolean existsByDepartmentId(Long departmentId);

    @Query("select f from Faculty f where lower(f.facultyId) like lower(concat('%', :q, '%')) "
         + "or lower(f.name) like lower(concat('%', :q, '%')) order by f.facultyId")
    List<Faculty> search(@Param("q") String q);
}
