package com.interviewpilot.ai.repository;

import com.interviewpilot.ai.document.LlmRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LlmRequestRepository extends MongoRepository<LlmRequest, String> {
}
