package com.interviewpilot.ai.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "ai_conversations")
public class AiConversation {

    @Id
    private String id;
    private Long userId;
    private Long sessionId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
