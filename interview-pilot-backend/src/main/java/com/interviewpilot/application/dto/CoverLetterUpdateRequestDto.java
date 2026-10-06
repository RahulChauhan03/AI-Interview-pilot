package com.interviewpilot.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoverLetterUpdateRequestDto {

    @NotEmpty(message = "The cover letter needs at least one paragraph")
    @Size(max = 8, message = "At most 8 paragraphs")
    private List<@NotBlank(message = "Paragraphs cannot be empty") @Size(max = 2000, message = "Paragraphs are limited to 2,000 characters") String> paragraphs;
}
