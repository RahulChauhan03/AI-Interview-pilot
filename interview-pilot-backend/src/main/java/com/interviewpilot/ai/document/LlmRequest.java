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
@Document(collection = "llm_requests")
public class LlmRequest {

    @Id
    private String id;
    private Long sessionId;
    private String provider;
    private String modelName;
    private String requestPayload;
    private String responsePayload;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long executionTimeMs;
    private String status;
    private LocalDateTime createdAt;
}
