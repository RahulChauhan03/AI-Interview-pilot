package com.interviewpilot.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.interviewpilot.application.ApplicationTestData;
import com.interviewpilot.application.document.ResumeFacts;
import com.interviewpilot.application.dto.SkillGapDto;
import com.interviewpilot.interview.dto.InterviewAnswerDto;
import com.interviewpilot.interview.dto.InterviewQuestionDto;
import com.interviewpilot.interview.dto.InterviewResponseDto;
import com.interviewpilot.interview.service.InterviewService;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.resume.service.ResumeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SkillGapServiceImplTest {

    private static final long OWNER = 1L;

    @Mock private JobDescriptionService jobDescriptionService;
    @Mock private InterviewService interviewService;
    @Mock private ResumeService resumeService;
    @InjectMocks private SkillGapServiceImpl service;

    private static InterviewQuestionDto question(String category, Double score, String improvement) {
        InterviewAnswerDto answer = score == null ? null : InterviewAnswerDto.builder().score(score)
                .improvements(improvement == null ? List.of() : List.of(improvement)).strengths(List.of()).build();
        return InterviewQuestionDto.builder().question("Q").category(category).answer(answer).build();
    }

    private static JobDescriptionResponseDto job(long id, String text) {
        return JobDescriptionResponseDto.builder().id(id).jobTitle("Backend Engineer").jobDescription(text).build();
    }

    @Test
    void classifiesSkillsFromTheResumeTheMatchAndInterviewScores() {
        ResumeFacts resume = ResumeFacts.from(ApplicationTestData.parsedResume(), "");
        ResumeMatchResponseDto match = ResumeMatchResponseDto.builder().missingSkills(List.of("AWS", "Kafka", "AWS")).build();
        InterviewResponseDto interview = InterviewResponseDto.builder().questions(List.of(
                question("SYSTEM_DESIGN", 40.0, "Discuss trade-offs between consistency and availability."),
                question("SYSTEM_DESIGN", 60.0, null),
                question("JAVA", 85.0, null),
                question("DATABASES", null, null))).build(); // unanswered: no evidence either way

        SkillGapDto gaps = service.forJob(job(10, "We use Java, Spring Boot and MySQL on AWS."), resume, match, List.of(interview));

        assertThat(gaps.strong()).extracting(SkillGapDto.Skill::name).containsExactly("Java", "Spring Boot", "MySQL");
        assertThat(gaps.developing()).singleElement().satisfies(skill -> {
            assertThat(skill.name()).isEqualTo("System Design");
            assertThat(skill.score()).isEqualTo(50.0);
        });
        assertThat(gaps.missing()).extracting(SkillGapDto.Skill::name).containsExactly("AWS", "Kafka");
        assertThat(gaps.recommendations()).hasSize(3)
                .anySatisfy(text -> assertThat(text).startsWith("AWS:"))
                .contains("Discuss trade-offs between consistency and availability.");
        assertThat(gaps.recommendations()).noneMatch(text -> text.contains("Databases"));
    }

    @Test
    void withoutResumeMatchOrInterviewsThereAreNoGapsOrAdvice() {
        SkillGapDto gaps = service.forJob(job(10, "Java"), null, null, List.of());

        assertThat(gaps.strong()).isEmpty();
        assertThat(gaps.developing()).isEmpty();
        assertThat(gaps.missing()).isEmpty();
        assertThat(gaps.recommendations()).isEmpty();
    }

    @Test
    void overallCountsGapsAcrossJobs() {
        when(jobDescriptionService.findAllForUser(OWNER)).thenReturn(List.of(job(10, "Java and AWS"), job(11, "AWS and Docker"), job(12, "Go")));
        when(jobDescriptionService.findAllMatchesForUser(OWNER)).thenReturn(List.of(
                ResumeMatchResponseDto.builder().jobDescriptionId(11L).resumeId(5L).missingSkills(List.of("aws", "Docker")).build(),
                ResumeMatchResponseDto.builder().jobDescriptionId(10L).resumeId(5L).missingSkills(List.of("AWS")).build(),
                ResumeMatchResponseDto.builder().jobDescriptionId(10L).resumeId(5L).missingSkills(List.of("Old gap")).build()));
        when(resumeService.findParsedByIdForUser(5L, OWNER)).thenReturn(ApplicationTestData.parsedResume());
        when(interviewService.findAllWithQuestionsForUser(OWNER)).thenReturn(List.of());

        SkillGapDto gaps = service.overall(OWNER);

        assertThat(gaps.missing()).extracting(SkillGapDto.Skill::name, SkillGapDto.Skill::jobs)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("AWS", 2), org.assertj.core.groups.Tuple.tuple("Docker", 1));
        assertThat(gaps.strong()).extracting(SkillGapDto.Skill::name).containsExactly("Java");
        assertThat(gaps.recommendations().get(0)).startsWith("AWS: required by 2 of your jobs");
    }
}
