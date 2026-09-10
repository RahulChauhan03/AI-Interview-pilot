package com.interviewpilot.jobdescription.dto;

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
public class JobDescriptionRequestDto {
    private Long userId;
    private String companyName;
    private String jobTitle;
    private String jobDescription;
}
