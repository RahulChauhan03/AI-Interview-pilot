package com.interviewpilot.resume.repository;

import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.entity.ResumeStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
    Optional<Resume> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    long countByIsDeletedFalse();
    long countByStatusInAndIsDeletedFalse(Collection<ResumeStatus> statuses);
    List<Resume> findTop15ByIsDeletedFalseOrderByUpdatedAtDesc();

    List<Resume> findByStatusInAndIsDeletedFalseAndUpdatedAtBefore(Collection<ResumeStatus> statuses, LocalDateTime before);

    /**
     * Atomically marks a resume as PROCESSING if it is waiting (UPLOADED) or stuck (PROCESSING since
     * before {@code staleBefore}). Returns 0 when another worker already owns it, so a resume is never
     * processed twice at the same time.
     */
    @Modifying
    @Transactional
    @Query("""
            update Resume r
               set r.status = com.interviewpilot.resume.entity.ResumeStatus.PROCESSING, r.updatedAt = :now
             where r.id = :id
               and r.isDeleted = false
               and (r.status = com.interviewpilot.resume.entity.ResumeStatus.UPLOADED
                    or (r.status = com.interviewpilot.resume.entity.ResumeStatus.PROCESSING and r.updatedAt < :staleBefore))
            """)
    int claimForProcessing(@Param("id") Long id, @Param("now") LocalDateTime now,
                           @Param("staleBefore") LocalDateTime staleBefore);
}
