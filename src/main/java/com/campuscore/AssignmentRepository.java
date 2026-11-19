package com.campuscore;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findBySectionIdOrderByDueDateAscIdAsc(Long sectionId);

    List<Assignment> findByFacultyFacultyIdOrderByDueDateAscIdAsc(String facultyId);

    boolean existsBySectionId(Long sectionId);

    boolean existsBySubjectId(Long subjectId);

    long countByFacultyFacultyId(String facultyId);

    long countByAttachmentStoredName(String attachmentStoredName);

    List<Assignment> findByFacultyFacultyIdAndSubjectIdAndSectionId(String facultyId, Long subjectId, Long sectionId);

    void deleteByFacultyFacultyId(String facultyId);

    void deleteByFacultyFacultyIdAndSubjectIdAndSectionId(String facultyId, Long subjectId, Long sectionId);
}
