package com.interviewpilot.ai.repository;

import com.interviewpilot.ai.document.DocumentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentChunkRepository extends MongoRepository<DocumentChunk, String> {
}
