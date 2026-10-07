package com.interviewpilot.application.service.impl;

import com.interviewpilot.application.document.ClaimGuard;
import com.interviewpilot.application.document.ResumeFacts;
import com.interviewpilot.application.dto.SkillGapDto;
import com.interviewpilot.application.dto.SkillGapDto.Skill;
import com.interviewpilot.application.service.SkillGapService;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.interview.dto.InterviewAnswerDto;
import com.interviewpilot.interview.dto.InterviewQuestionDto;
import com.interviewpilot.interview.dto.InterviewResponseDto;
import com.interviewpilot.interview.service.InterviewService;
import com.interviewpilot.jobdescription.dto.JobDescriptionResponseDto;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.resume.service.ResumeService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SkillGapServiceImpl implements SkillGapService {

    /** Interview categories averaging below this are "developing". */
    static final double DEVELOPING_BELOW = 70;

    private final JobDescriptionService jobDescriptionService;
    private final InterviewService interviewService;
    private final ResumeService resumeService;

    @Override
    public SkillGapDto forJob(JobDescriptionResponseDto job, ResumeFacts resume, ResumeMatchResponseDto match,
                              List<InterviewResponseDto> interviews) {
        Map<String, double[]> categories = categoryScores(interviews);
        List<Skill> developing = developing(categories);
        Set<String> developingKeys = new LinkedHashSet<>(developing.stream().map(skill -> key(skill.name())).toList());

        Map<String, Skill> strong = new LinkedHashMap<>();
        String jobText = job.getJobDescription().toLowerCase(Locale.ROOT);
        if (resume != null) {
            for (String skill : resume.skills()) {
                if (ClaimGuard.containsTerm(jobText, skill.toLowerCase(Locale.ROOT)) && !developingKeys.contains(key(skill))) {
                    strong.putIfAbsent(key(skill), new Skill(skill, 1, null));
                }
            }
        }
        categories.forEach((category, totals) -> {
            double average = totals[0] / totals[1];
            if (average >= DEVELOPING_BELOW) strong.putIfAbsent(key(category), new Skill(pretty(category), 1, round(average)));
        });

        List<Skill> missing = match == null ? List.of()
                : match.getMissingSkills().stream().distinct().map(name -> new Skill(name, 1, null)).toList();

        List<String> recommendations = new ArrayList<>();
        missing.stream().limit(4).forEach(skill -> recommendations.add(skill.name()
                + ": the job asks for this and it is not on your resume. Learn the basics and build a small project with it "
                + "before you list it."));
        recommendations.addAll(improvementNotes(interviews, developingKeys, 4));
        return new SkillGapDto(new ArrayList<>(strong.values()), developing, missing, recommendations);
    }

    @Override
    @Transactional(readOnly = true)
    public SkillGapDto overall(Long userId) {
        Map<String, Skill> strong = new HashMap<>();
        Map<String, Skill> missing = new HashMap<>();
        Map<Long, Optional<ResumeFacts>> resumes = new HashMap<>();
        // One query each for matches and interviews instead of two per job description.
        Map<Long, ResumeMatchResponseDto> latestMatches = new HashMap<>();
        jobDescriptionService.findAllMatchesForUser(userId) // newest first
                .forEach(match -> latestMatches.putIfAbsent(match.getJobDescriptionId(), match));
        List<InterviewResponseDto> allInterviews = interviewService.findAllWithQuestionsForUser(userId).stream()
                .filter(interview -> interview.getJobDescriptionId() != null).toList();

        for (JobDescriptionResponseDto job : jobDescriptionService.findAllForUser(userId)) {
            ResumeMatchResponseDto match = latestMatches.get(job.getId());
            ResumeFacts resume = match == null ? null
                    : resumes.computeIfAbsent(match.getResumeId(), id -> facts(id, userId)).orElse(null);
            SkillGapDto gaps = forJob(job, resume, match, List.of()); // interview categories are combined below
            gaps.strong().forEach(skill -> count(strong, skill.name()));
            gaps.missing().forEach(skill -> count(missing, skill.name()));
        }
        Map<String, double[]> categories = categoryScores(allInterviews);
        List<Skill> developing = developing(categories);
        Set<String> developingKeys = new LinkedHashSet<>(developing.stream().map(skill -> key(skill.name())).toList());
        developingKeys.forEach(strong::remove);
        categories.forEach((category, totals) -> {
            double average = totals[0] / totals[1];
            if (average >= DEVELOPING_BELOW) strong.putIfAbsent(key(category), new Skill(pretty(category), 1, round(average)));
        });

        Comparator<Skill> mostCommon = Comparator.comparingInt(Skill::jobs).reversed().thenComparing(Skill::name);
        List<Skill> missingSorted = missing.values().stream().sorted(mostCommon).toList();
        List<String> recommendations = new ArrayList<>();
        missingSorted.stream().limit(5).forEach(skill -> recommendations.add(skill.name() + ": required by " + skill.jobs()
                + " of your jobs but not on your resume. Learn it and build a small project "
                + "before you list it."));
        developing.stream().limit(3).forEach(skill -> recommendations.add("Practise " + skill.name()
                + " questions: your average is " + skill.score().intValue() + "/100."));
        recommendations.addAll(improvementNotes(allInterviews, developingKeys, 3));
        return new SkillGapDto(strong.values().stream().sorted(mostCommon).toList(), developing, missingSorted, recommendations);
    }

    /** category -> {sum of answer scores, number of answers}; only answered questions count as evidence. */
    private Map<String, double[]> categoryScores(List<InterviewResponseDto> interviews) {
        Map<String, double[]> totals = new LinkedHashMap<>();
        for (InterviewResponseDto interview : interviews) {
            for (InterviewQuestionDto question : interview.getQuestions() == null ? List.<InterviewQuestionDto>of() : interview.getQuestions()) {
                InterviewAnswerDto answer = question.getAnswer();
                if (answer != null && answer.getScore() != null && question.getCategory() != null) {
                    double[] total = totals.computeIfAbsent(question.getCategory(), category -> new double[2]);
                    total[0] += answer.getScore();
                    total[1] += 1;
                }
            }
        }
        return totals;
    }

    private List<Skill> developing(Map<String, double[]> categories) {
        List<Skill> developing = new ArrayList<>();
        categories.forEach((category, totals) -> {
            double average = totals[0] / totals[1];
            if (average < DEVELOPING_BELOW) developing.add(new Skill(pretty(category), 1, round(average)));
        });
        developing.sort(Comparator.comparing(Skill::score));
        return developing;
    }

    /** The AI's own improvement notes on lower-scoring answers in the developing categories. */
    private List<String> improvementNotes(List<InterviewResponseDto> interviews, Set<String> developingKeys, int limit) {
        Set<String> notes = new LinkedHashSet<>();
        for (InterviewResponseDto interview : interviews) {
            for (InterviewQuestionDto question : interview.getQuestions() == null ? List.<InterviewQuestionDto>of() : interview.getQuestions()) {
                InterviewAnswerDto answer = question.getAnswer();
                if (answer != null && answer.getScore() != null && answer.getScore() < DEVELOPING_BELOW
                        && question.getCategory() != null && developingKeys.contains(key(question.getCategory()))) {
                    notes.addAll(answer.getImprovements());
                }
            }
        }
        return notes.stream().limit(limit).toList();
    }

    private Optional<ResumeFacts> facts(Long resumeId, Long userId) {
        try {
            return Optional.of(ResumeFacts.from(resumeService.findParsedByIdForUser(resumeId, userId), ""));
        } catch (ResourceNotFoundException exception) {
            return Optional.empty(); // deleted or not parsed
        }
    }

    private static void count(Map<String, Skill> counts, String name) {
        counts.merge(key(name), new Skill(name, 1, null), (existing, added) -> new Skill(existing.name(), existing.jobs() + 1, null));
    }

    /** "SPRING_BOOT", "Spring Boot" and "spring-boot" share the key "springboot". */
    static String key(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}+#]", "");
    }

    private static String pretty(String category) {
        String spaced = category.replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        for (String word : spaced.split(" +")) {
            if (!word.isEmpty()) out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static Double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
