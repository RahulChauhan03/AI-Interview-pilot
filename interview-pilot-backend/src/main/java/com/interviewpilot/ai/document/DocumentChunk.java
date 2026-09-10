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
@Document(collection = "document_chunks")
public class DocumentChunk {

    @Id
    private String id;
    private String sourceType;
    private Long sourceId;
    private Integer chunkIndex;
    private String content;
    private LocalDateTime createdAt;
}
