package com.interviewpilot.interview.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.ConflictException;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.interview.dto.InterviewAnswerDto;
import com.interviewpilot.interview.dto.InterviewQuestionDto;
import com.interviewpilot.interview.dto.InterviewRequestDto;
import com.interviewpilot.interview.dto.InterviewResponseDto;
import com.interviewpilot.interview.entity.InterviewAnswer;
import com.interviewpilot.interview.entity.InterviewQuestion;
import com.interviewpilot.interview.entity.InterviewSession;
import com.interviewpilot.interview.entity.InterviewStatus;
import com.interviewpilot.interview.repository.InterviewAnswerRepository;
import com.interviewpilot.interview.repository.InterviewQuestionRepository;
import com.interviewpilot.interview.repository.InterviewSessionRepository;
import com.interviewpilot.interview.service.AnswerScoringService;
import com.interviewpilot.interview.service.InterviewService;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.resume.ai.AnswerEvaluation;
import com.interviewpilot.resume.ai.GeneratedQuestions;
import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.service.InterviewGenerationService;
import com.interviewpilot.resume.service.ResumeService;
import com.interviewpilot.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interview lifecycle. AI calls (question generation, answer scoring) run before anything is saved and
 * outside a database transaction, so a slow or failed AI call never leaves partial data or holds a connection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    static final int DEFAULT_QUESTION_COUNT = 5;
    private static final int MAX_SCORE_PER_QUESTION = 100;

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final JobDescriptionService jobDescriptionService;
    private final ResumeService resumeService;
    private final InterviewGenerationService interviewGenerationService;
    private final AnswerScoringService answerScoringService;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewResponseDto create(InterviewRequestDto request, Long userId) {
        JobDescription jobDescription = jobDescriptionService.findOwned(request.getJobDescriptionId(), userId);
        ParsedResume parsedResume = resumeService.findParsedByIdForUser(request.getResumeId(), userId);
        Resume resume = resumeService.findByIdForUser(request.getResumeId(), userId);
        int questionCount = request.getQuestionCount() == null ? DEFAULT_QUESTION_COUNT : request.getQuestionCount();

        List<String> missingSkills = jobDescriptionService.findLatestMissingSkills(jobDescription.getId(), resume.getId(), userId);

        List<GeneratedQuestions.Question> generated = interviewGenerationService.generateQuestions(
                parsedResume.getCleanText(), jobDescription, questionCount, missingSkills);

        InterviewSession session = InterviewSession.builder()
                .user(userRepository.getReferenceById(userId))
                .resume(resume)
                .jobDescription(jobDescription)
                .status(InterviewStatus.IN_PROGRESS.name())
                .startedAt(LocalDateTime.now())
                .build();
        for (int i = 0; i < generated.size(); i++) {
            GeneratedQuestions.Question question = generated.get(i);
            session.getQuestions().add(InterviewQuestion.builder()
                    .session(session)
                    .question(question.question())
                    .category(question.category())
                    .difficulty(question.difficulty())
                    .sequenceNumber(i + 1)
                    .build());
        }
        InterviewSession saved = sessionRepository.save(session); // questions are saved with it
        log.info("Interview created: sessionId={}, questions={}", saved.getId(), generated.size());
        return toDto(saved, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponseDto> findAllForUser(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(session -> toDto(session, false)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponseDto> findAllForJob(Long jobDescriptionId, Long userId) {
        return sessionRepository.findByJobDescriptionIdAndUserIdOrderByCreatedAtDesc(jobDescriptionId, userId).stream()
                .map(session -> toDto(session, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponseDto findByIdForUser(Long id, Long userId) {
        return toDto(findOwned(id, userId), true);
    }

    /** Returns null when every question is answered or the interview is finished. */
    @Override
    @Transactional(readOnly = true)
    public InterviewQuestionDto findNextQuestion(Long id, Long userId) {
        InterviewSession session = findOwned(id, userId);
        if (!InterviewStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            return null;
        }
        return session.getQuestions().stream()
                .filter(question -> question.getAnswers().isEmpty())
                .min(Comparator.comparing(InterviewQuestion::getSequenceNumber))
                .map(this::toQuestionDto)
                .orElse(null);
    }

    @Override
    public InterviewAnswerDto submitAnswer(Long sessionId, Long questionId, String answer, Long userId) {
        InterviewSession session = findOwned(sessionId, userId);
        if (InterviewStatus.COMPLETED.name().equals(session.getStatus())) {
            throw new ConflictException("This interview is already finished");
        }
        InterviewQuestion question = questionRepository.findByIdAndSessionId(questionId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        if (answerRepository.existsByQuestionId(questionId)) {
            throw new ConflictException("This question has already been answered");
        }
        String answerText = answer.trim();
        String jobTitle = session.getJobDescription() == null
                ? "the target role" // job_description_id is nullable in the schema
                : jobDescriptionService.findOwned(session.getJobDescription().getId(), userId).getJobTitle();

        AnswerEvaluation evaluation = answerScoringService.evaluate(
                question.getQuestion(), question.getCategory(), question.getDifficulty(), jobTitle, answerText);

        InterviewAnswer saved = answerRepository.save(InterviewAnswer.builder()
                .question(question)
                .answer(answerText)
                .score((double) evaluation.score())
                .aiFeedback(toJson(evaluation))
                .build());
        log.info("Interview answer scored: sessionId={}, questionId={}, score={}", sessionId, questionId, evaluation.score());
        return toAnswerDto(saved);
    }

    /** Idempotent. Unanswered questions count as 0, so finishing early does not inflate the score. */
    @Override
    @Transactional
    public InterviewResponseDto complete(Long sessionId, Long userId) {
        InterviewSession session = findOwned(sessionId, userId);
        if (!InterviewStatus.COMPLETED.name().equals(session.getStatus())) {
            double points = answerRepository.findByQuestionSessionId(sessionId).stream()
                    .mapToDouble(answer -> answer.getScore() == null ? 0 : answer.getScore())
                    .sum();
            int maxPoints = session.getQuestions().size() * MAX_SCORE_PER_QUESTION;
            session.setOverallScore(maxPoints == 0 ? 0 : Math.round(points * 1000 / maxPoints) / 10.0);
            session.setStatus(InterviewStatus.COMPLETED.name());
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Interview completed: sessionId={}, overallScore={}", sessionId, session.getOverallScore());
        }
        return toDto(session, true);
    }

    private InterviewSession findOwned(Long id, Long userId) {
        return sessionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
    }

    private InterviewResponseDto toDto(InterviewSession session, boolean includeQuestions) {
        List<InterviewQuestion> questions = session.getQuestions().stream()
                .sorted(Comparator.comparing(InterviewQuestion::getSequenceNumber))
                .toList();
        boolean inProgress = InterviewStatus.IN_PROGRESS.name().equals(session.getStatus());
        Long nextQuestionId = inProgress
                ? questions.stream().filter(q -> q.getAnswers().isEmpty()).map(InterviewQuestion::getId).findFirst().orElse(null)
                : null;
        JobDescription jobDescription = session.getJobDescription();
        Resume resume = session.getResume();
        return InterviewResponseDto.builder()
                .id(session.getId())
                .userId(session.getUser().getId())
                .resumeId(resume == null ? null : resume.getId())
                .jobDescriptionId(jobDescription == null ? null : jobDescription.getId())
                .status(session.getStatus())
                .overallScore(session.getOverallScore())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .jobTitle(jobDescription == null ? null : jobDescription.getJobTitle())
                .companyName(jobDescription == null ? null : jobDescription.getCompanyName())
                .resumeFileName(resume == null ? null : resume.getOriginalFileName())
                .totalQuestions(questions.size())
                .answeredQuestions((int) questions.stream().filter(q -> !q.getAnswers().isEmpty()).count())
                .nextQuestionId(nextQuestionId)
                .questions(includeQuestions ? questions.stream().map(this::toQuestionDto).toList() : null)
                .build();
    }

    private InterviewQuestionDto toQuestionDto(InterviewQuestion question) {
        return InterviewQuestionDto.builder()
                .id(question.getId())
                .sequenceNumber(question.getSequenceNumber())
                .question(question.getQuestion())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .answer(question.getAnswers().isEmpty() ? null : toAnswerDto(question.getAnswers().get(0)))
                .build();
    }

    private InterviewAnswerDto toAnswerDto(InterviewAnswer answer) {
        AnswerEvaluation evaluation = fromJson(answer.getAiFeedback());
        return InterviewAnswerDto.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion().getId())
                .answer(answer.getAnswer())
                .score(answer.getScore())
                .correctness(evaluation == null ? null : evaluation.correctness())
                .relevance(evaluation == null ? null : evaluation.relevance())
                .feedback(evaluation == null ? answer.getAiFeedback() : evaluation.feedback())
                .strengths(evaluation == null ? List.of() : evaluation.strengths())
                .improvements(evaluation == null ? List.of() : evaluation.improvements())
                .createdAt(answer.getCreatedAt())
                .build();
    }

    /** interview_answers.ai_feedback holds the validated evaluation as JSON (correctness, relevance, feedback, strengths, improvements). */
    private String toJson(AnswerEvaluation evaluation) {
        try {
            return objectMapper.writeValueAsString(evaluation);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize answer feedback", exception);
        }
    }

    private AnswerEvaluation fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AnswerEvaluation.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
