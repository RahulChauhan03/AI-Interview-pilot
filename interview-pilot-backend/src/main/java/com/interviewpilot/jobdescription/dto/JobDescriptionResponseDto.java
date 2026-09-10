package com.interviewpilot.jobdescription.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescriptionResponseDto {
    private Long id;
    private Long userId;
    private String companyName;
    private String jobTitle;
    private String jobDescription;
    private LocalDateTime createdAt;
}
