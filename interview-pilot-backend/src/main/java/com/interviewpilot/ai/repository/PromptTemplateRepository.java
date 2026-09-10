package com.interviewpilot.ai.repository;

import com.interviewpilot.ai.document.PromptTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PromptTemplateRepository extends MongoRepository<PromptTemplate, String> {
}
