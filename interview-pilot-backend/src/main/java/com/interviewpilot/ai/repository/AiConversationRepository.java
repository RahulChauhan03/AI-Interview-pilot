package com.interviewpilot.ai.repository;

import com.interviewpilot.ai.document.AiConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiConversationRepository extends MongoRepository<AiConversation, String> {
}
