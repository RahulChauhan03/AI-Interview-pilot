package com.interviewpilot.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusRequestDto {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "SAVED|PREPARING|APPLIED|ASSESSMENT|INTERVIEW|OFFER|REJECTED|WITHDRAWN", message = "Unknown status")
    private String status;
}
