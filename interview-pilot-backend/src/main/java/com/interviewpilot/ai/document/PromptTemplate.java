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
@Document(collection = "prompt_templates")
public class PromptTemplate {

    @Id
    private String id;
    private String name;
    private String version;
    private String content;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
