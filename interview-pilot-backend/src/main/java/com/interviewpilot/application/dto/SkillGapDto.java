package com.interviewpilot.application.dto;

import java.util.List;

/**
 * Strong, developing and missing skills, computed only from real data: resume skills that the job description
 * asks for, the match analysis's missing skills, and interview scores per question category.
 */
public record SkillGapDto(List<Skill> strong, List<Skill> developing, List<Skill> missing, List<String> recommendations) {

    /** {@code jobs}: how many job descriptions this applies to; {@code score}: average interview score, if any. */
    public record Skill(String name, int jobs, Double score) {
    }
}
