package com.campuscore;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {
    @Query("select t from TeachingAssignment t order by t.faculty.facultyId, t.subject.code, t.section.id")
    List<TeachingAssignment> findAllOrdered();

    @Query("select t from TeachingAssignment t where t.faculty.facultyId = :facultyId "
         + "order by t.subject.name, t.section.yearOfStudy, t.section.name")
    List<TeachingAssignment> forFaculty(@Param("facultyId") String facultyId);

    @Query("select t from TeachingAssignment t where t.section.id = :sectionId order by t.subject.name")
    List<TeachingAssignment> forSection(@Param("sectionId") Long sectionId);

    long countByFacultyFacultyIdAndSubjectIdAndSectionId(String facultyId, Long subjectId, Long sectionId);

    boolean existsBySectionId(Long sectionId);

    boolean existsBySubjectId(Long subjectId);

    void deleteByFacultyFacultyId(String facultyId);
}
