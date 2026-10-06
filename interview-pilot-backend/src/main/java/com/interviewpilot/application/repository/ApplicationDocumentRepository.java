package com.interviewpilot.application.repository;

import com.interviewpilot.application.entity.ApplicationDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {
    Optional<ApplicationDocument> findByApplicationIdAndDocumentType(Long applicationId, String documentType);
    List<ApplicationDocument> findByApplicationId(Long applicationId);
}
