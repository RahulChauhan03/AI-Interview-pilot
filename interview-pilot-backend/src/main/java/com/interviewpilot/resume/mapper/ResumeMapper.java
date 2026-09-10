package com.interviewpilot.resume.mapper;

import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.dto.ParsedResumeDto;
import com.interviewpilot.resume.dto.ResumeResponseDto;
import com.interviewpilot.resume.dto.ResumeSummaryDto;
import com.interviewpilot.resume.dto.ResumeUploadResponseDto;
import com.interviewpilot.resume.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {
    public ResumeUploadResponseDto toUploadResponse(Resume resume) {
        return ResumeUploadResponseDto.builder().id(resume.getId()).originalFileName(resume.getOriginalFileName())
                .status(resume.getStatus()).uploadTime(resume.getUploadTime()).build();
    }

    public ResumeResponseDto toResponse(Resume resume) {
        return ResumeResponseDto.builder().id(resume.getId()).userId(resume.getUser().getId())
                .originalFileName(resume.getOriginalFileName()).storedFileName(resume.getStoredFileName())
                .fileExtension(resume.getFileExtension()).mimeType(resume.getMimeType()).fileSize(resume.getFileSize())
                .storagePath(resume.getStoragePath()).status(resume.getStatus()).uploadTime(resume.getUploadTime())
                .parseTime(resume.getParseTime()).processingTime(resume.getProcessingTime()).language(resume.getLanguage())
                .summary(resume.getSummary()).createdAt(resume.getCreatedAt()).updatedAt(resume.getUpdatedAt()).build();
    }

    public ResumeSummaryDto toSummary(Resume resume) {
        return ResumeSummaryDto.builder().id(resume.getId()).originalFileName(resume.getOriginalFileName())
                .status(resume.getStatus()).summary(resume.getSummary()).uploadTime(resume.getUploadTime()).build();
    }

    public ParsedResumeDto toParsedResponse(ParsedResume parsed) {
        return ParsedResumeDto.builder().resumeId(parsed.getResumeId()).personalInformation(parsed.getPersonalInformation())
                .experience(parsed.getExperience()).education(parsed.getEducation()).skills(parsed.getSkills())
                .projects(parsed.getProjects()).certifications(parsed.getCertifications()).achievements(parsed.getAchievements())
                .languages(parsed.getLanguages()).softSkills(parsed.getSoftSkills()).technicalSkills(parsed.getTechnicalSkills())
                .companies(parsed.getCompanies()).designations(parsed.getDesignations()).yearsOfExperience(parsed.getYearsOfExperience())
                .summary(parsed.getSummary()).keywords(parsed.getKeywords()).createdAt(parsed.getCreatedAt()).build();
    }
}
