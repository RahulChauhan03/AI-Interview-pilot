package com.interviewpilot.resume.document;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ParsedResumeRepository extends MongoRepository<ParsedResume, String> {

    Optional<ParsedResume> findByResumeId(Long resumeId);
}
