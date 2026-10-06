package com.interviewpilot.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDocumentRequestDto {
    /** Base resume; defaults to the application's resume. */
    private Long resumeId;
}
