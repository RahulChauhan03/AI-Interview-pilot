package com.interviewpilot.application.repository;

import com.interviewpilot.application.entity.ApplicationDocument;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {
    Optional<ApplicationDocument> findByApplicationIdAndDocumentType(Long applicationId, String documentType);
    List<ApplicationDocument> findByApplicationId(Long applicationId);

    /** When each document of these applications was last written, without loading the documents' content. */
    @Query("select d.application.id as applicationId, d.documentType as documentType, "
            + "coalesce(d.updatedAt, d.createdAt) as writtenAt from ApplicationDocument d where d.application.id in :ids")
    List<DocumentTimestamp> findTimestamps(@Param("ids") Collection<Long> applicationIds);

    interface DocumentTimestamp {
        Long getApplicationId();
        String getDocumentType();
        LocalDateTime getWrittenAt();
    }
}
