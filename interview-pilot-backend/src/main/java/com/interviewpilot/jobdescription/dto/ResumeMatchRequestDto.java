package com.interviewpilot.jobdescription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeMatchRequestDto {

    @NotNull(message = "Resume is required")
    private Long resumeId;
}
