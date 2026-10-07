package com.interviewpilot.interview.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.ConflictException;
import com.interviewpilot.exception.InvalidAiResponseException;
import com.interviewpilot.exception.OllamaUnavailableException;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.interview.dto.InterviewAnswerDto;
import com.interviewpilot.common.concurrency.KeyedLocks;
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
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.resume.ai.AnswerEvaluation;
import com.interviewpilot.resume.ai.GeneratedQuestions;
import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.service.InterviewGenerationService;
import com.interviewpilot.resume.service.ResumeService;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    private static final long OWNER = 1L;
    private static final long OTHER_USER = 2L;

    @Mock private InterviewSessionRepository sessionRepository;
    @Mock private InterviewQuestionRepository questionRepository;
    @Mock private InterviewAnswerRepository answerRepository;
    @Mock private UserRepository userRepository;
    @Mock private JobDescriptionService jobDescriptionService;
    @Mock private ResumeService resumeService;
    @Mock private InterviewGenerationService interviewGenerationService;
    @Mock private AnswerScoringService answerScoringService;

    private InterviewServiceImpl service;
    private final User owner = User.builder().id(OWNER).build();
    private final JobDescription jobDescription = JobDescription.builder().id(10L).user(owner)
            .companyName("Acme").jobTitle("Backend Engineer").jobDescription("Java").build();
    private final Resume resume = Resume.builder().id(5L).user(owner).originalFileName("cv.pdf").build();

    @BeforeEach
    void setUp() {
        service = new InterviewServiceImpl(sessionRepository, questionRepository, answerRepository, userRepository,
                jobDescriptionService, resumeService, interviewGenerationService, answerScoringService, new ObjectMapper(), new KeyedLocks());
    }

    @Test
    void createWithAnotherUsersJobDescriptionNeverCallsTheAi() {
        when(jobDescriptionService.findOwned(10L, OTHER_USER)).thenThrow(new ResourceNotFoundException("Job description not found"));

        assertThatThrownBy(() -> service.create(new InterviewRequestDto(5L, 10L, null), OTHER_USER))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(interviewGenerationService, sessionRepository);
    }

    @Test
    void createSavesSessionWithNumberedQuestions() {
        givenOwnedJobDescriptionAndResume();
        when(jobDescriptionService.findLatestMissingSkills(10L, 5L, OWNER)).thenReturn(List.of("Kafka"));
        when(interviewGenerationService.generateQuestions("resume text", jobDescription, 5, List.of("Kafka"))).thenReturn(List.of(
                new GeneratedQuestions.Question("Explain JPA.", "JAVA", "EASY"),
                new GeneratedQuestions.Question("Design a cache.", "SYSTEM_DESIGN", "HARD")));
        when(userRepository.getReferenceById(OWNER)).thenReturn(owner);
        when(sessionRepository.save(any(InterviewSession.class))).thenAnswer(invocation -> {
            InterviewSession session = invocation.getArgument(0);
            session.setId(100L);
            for (InterviewQuestion question : session.getQuestions()) {
                question.setId(200L + question.getSequenceNumber());
            }
            return session;
        });

        InterviewResponseDto created = service.create(new InterviewRequestDto(5L, 10L, null), OWNER);

        assertThat(created.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(created.getTotalQuestions()).isEqualTo(2);
        assertThat(created.getNextQuestionId()).isEqualTo(201L);
        assertThat(created.getJobTitle()).isEqualTo("Backend Engineer");
        assertThat(created.getQuestions()).extracting("sequenceNumber").containsExactly(1, 2);
        assertThat(created.getQuestions()).extracting("difficulty").containsExactly("EASY", "HARD");
    }

    @Test
    void failedQuestionGenerationSavesNoSession() {
        givenOwnedJobDescriptionAndResume();
        when(interviewGenerationService.generateQuestions(anyString(), any(), anyInt(), anyList()))
                .thenThrow(new InvalidAiResponseException("AI returned no interview questions"));

        assertThatThrownBy(() -> service.create(new InterviewRequestDto(5L, 10L, 3), OWNER))
                .isInstanceOf(InvalidAiResponseException.class);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void anotherUsersInterviewIsNotFound() {
        when(sessionRepository.findByIdAndUserId(100L, OTHER_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdForUser(100L, OTHER_USER))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Interview not found");
        assertThatThrownBy(() -> service.submitAnswer(100L, 201L, "answer", OTHER_USER))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(answerScoringService);
    }

    @Test
    void getReturnsQuestionsInOrderWithNextUnansweredQuestion() {
        InterviewSession session = session(InterviewStatus.IN_PROGRESS);
        session.getQuestions().get(0).getAnswers().add(InterviewAnswer.builder().id(300L)
                .question(session.getQuestions().get(0)).answer("JPA maps objects").score(80.0)
                .aiFeedback("{\"score\":80,\"correctness\":\"Right\",\"relevance\":\"On topic\",\"feedback\":\"Good\","
                        + "\"strengths\":[\"Clear\"],\"improvements\":[]}").build());
        when(sessionRepository.findByIdAndUserId(100L, OWNER)).thenReturn(Optional.of(session));

        InterviewResponseDto dto = service.findByIdForUser(100L, OWNER);

        assertThat(dto.getAnsweredQuestions()).isEqualTo(1);
        assertThat(dto.getNextQuestionId()).isEqualTo(202L);
        assertThat(dto.getQuestions().get(0).getAnswer().getFeedback()).isEqualTo("Good");
        assertThat(dto.getQuestions().get(0).getAnswer().getStrengths()).containsExactly("Clear");
        assertThat(dto.getQuestions().get(1).getAnswer()).isNull();
        assertThat(dto.getQuestions().get(0).getAnswer().getCorrectness()).isEqualTo("Right");
    }

    @Test
    void nextQuestionIsTheFirstUnansweredOneAndNoneWhenFinished() {
        InterviewSession session = session(InterviewStatus.IN_PROGRESS);
        session.getQuestions().get(0).getAnswers().add(InterviewAnswer.builder().id(300L)
                .question(session.getQuestions().get(0)).answer("x").score(50.0).build());
        InterviewSession finished = session(InterviewStatus.COMPLETED);
        when(sessionRepository.findByIdAndUserId(100L, OWNER)).thenReturn(Optional.of(session));
        when(sessionRepository.findByIdAndUserId(101L, OWNER)).thenReturn(Optional.of(finished));

        assertThat(service.findNextQuestion(100L, OWNER).getId()).isEqualTo(202L);
        assertThat(service.findNextQuestion(101L, OWNER)).isNull();
    }

    @Test
    void answeringAFinishedInterviewIsAConflict() {
        when(sessionRepository.findByIdAndUserId(100L, OWNER)).thenReturn(Optional.of(session(InterviewStatus.COMPLETED)));

        assertThatThrownBy(() -> service.submitAnswer(100L, 201L, "answer", OWNER)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(answerScoringService);
    }

    @Test
    void answeringAQuestionFromAnotherInterviewIsNotFound() {
        when(sessionRepository.findByIdAndUserId(100L, OWNER)).thenReturn(Optional.of(session(InterviewStatus.IN_PROGRESS)));
        when(questionRepository.findByIdAndSessionId(999L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitAnswer(100L, 999L, "answer", OWNER))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Question not found");
    }

    @Test
    void answeringTwiceIsAConflict() {
        InterviewSession session = session(InterviewStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUserId(100L, OWNER)).thenReturn(Optional.of(session));
        when(questionRepository.findByIdAndSessionId(201L, 100L)).thenReturn(Optional.of(session.getQuestions().get(0)));
        when(answerRepository.existsByQuestionId(201L)).thenReturn(true);

        assertThatThrownBy(() -> service.submitAnswer(100L, 201L, "answer", OWNER)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(answerScoringService);
    }

    @Test
    void failedScoringSavesNoAnswer() {
        givenAnswerableQuestion();
        when(answerScoringService.evaluate(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new OllamaUnavailableException("Ollama is unreachable", null));

        assertThatThrownBy(() -> service.submitAnswer(100L, 201L, "my answer", OWNER))
                .isInstanceOf(OllamaUnavailableException.class);
        verify(answerRepository, never()).save(any());
    }

    @Test
    void answerIsScoredThenSavedWithFeedback() {
        givenAnswerableQuestion();
        when(answerScoringService.evaluate("Explain JPA.", "JAVA", "EASY", "Backend Engineer", "my answer"))
                .thenReturn(new AnswerEvaluation(62, "Mostly right.", "Answers the question.", "Partly correct.",
                        List.of("Relevant"), List.of("Explain the persistence context")));
        when(answerRepository.save(any(InterviewAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewAnswerDto answer = service.submitAnswer(100L, 201L, "  my answer  ", OWNER);

        ArgumentCaptor<InterviewAnswer> saved = ArgumentCaptor.forClass(InterviewAnswer.class);
        verify(answerRepository).save(saved.capture());
        assertThat(saved.getValue().getAnswer()).isEqualTo("my answer");
        assertThat(saved.getValue().getScore()).isEqualTo(62.0);
        assertThat(answer.getFeedback()).isEqualTo("Partly correct.");
        assertThat(answer.getCorrectness()).isEqualTo("Mostly right.");
        assertThat(answer.getRelevance()).isEqualTo("Answers the question.");
        assertThat(answer.getImprovements()).containsExactly("Explain the persistence context");
    }

    @Test
    void completeCountsUnansweredQuestionsAsZero() {
        InterviewSession session = session(InterviewStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUserId(100L, OWNER)).thenReturn(Optional.of(session));
        when(answerRepository.findByQuestionSessionId(100L)).thenReturn(List.of(InterviewAnswer.builder().score(80.0).build()));

        InterviewResponseDto completed = service.complete(100L, OWNER);

        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(completed.getOverallScore()).isEqualTo(40.0); // 80 of 200 points
        assertThat(completed.getNextQuestionId()).isNull();
        assertThat(session.getCompletedAt()).isNotNull();
    }

    @Test
    void completingTwiceKeepsTheFirstResult() {
        InterviewSession session = session(InterviewStatus.COMPLETED);
        session.setOverallScore(55.0);
        when(sessionRepository.findByIdAndUserId(100L, OWNER)).thenReturn(Optional.of(session));

        assertThat(service.complete(100L, OWNER).getOverallScore()).isEqualTo(55.0);
        verify(sessionRepository, never()).save(any());
    }

    private void givenOwnedJobDescriptionAndResume() {
        when(jobDescriptionService.findOwned(10L, OWNER)).thenReturn(jobDescription);
        when(resumeService.findParsedByIdForUser(5L, OWNER)).thenReturn(ParsedResume.builder().cleanText("resume text").build());
        when(resumeService.findByIdForUser(5L, OWNER)).thenReturn(resume);
    }

    private void givenAnswerableQuestion() {
        InterviewSession session = session(InterviewStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUserId(100L, OWNER)).thenReturn(Optional.of(session));
        when(questionRepository.findByIdAndSessionId(201L, 100L)).thenReturn(Optional.of(session.getQuestions().get(0)));
        when(answerRepository.existsByQuestionId(201L)).thenReturn(false);
        when(jobDescriptionService.findOwned(10L, OWNER)).thenReturn(jobDescription);
    }

    private InterviewSession session(InterviewStatus status) {
        InterviewSession session = InterviewSession.builder().id(100L).user(owner).resume(resume)
                .jobDescription(jobDescription).status(status.name()).questions(new ArrayList<>()).build();
        session.getQuestions().add(question(session, 202L, 2, "Design a cache.", "SYSTEM_DESIGN", "HARD"));
        session.getQuestions().add(0, question(session, 201L, 1, "Explain JPA.", "JAVA", "EASY"));
        return session;
    }

    private InterviewQuestion question(InterviewSession session, Long id, int sequence, String text, String category, String difficulty) {
        return InterviewQuestion.builder().id(id).session(session).sequenceNumber(sequence)
                .question(text).category(category).difficulty(difficulty).answers(new ArrayList<>()).build();
    }

    @Test
    void repeatedCreateReturnsTheInterviewInProgressWithoutCallingTheAi() {
        when(jobDescriptionService.findOwned(10L, OWNER)).thenReturn(jobDescription);
        InterviewSession inProgress = InterviewSession.builder().id(100L).user(owner).jobDescription(jobDescription)
                .status("IN_PROGRESS").startedAt(java.time.LocalDateTime.now()).build();
        when(sessionRepository.findFirstByUserIdAndJobDescriptionIdAndResumeIdAndStatusOrderByCreatedAtDesc(OWNER, 10L, 5L, "IN_PROGRESS"))
                .thenReturn(java.util.Optional.of(inProgress));

        InterviewResponseDto result = service.create(new InterviewRequestDto(5L, 10L, 5), OWNER);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.isReused()).isTrue();
        verifyNoInteractions(interviewGenerationService);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void deleteRemovesOnlyTheOwnersInterview() {
        InterviewSession session = InterviewSession.builder().id(100L).user(owner).build();
        when(sessionRepository.findByIdAndUserId(100L, OWNER)).thenReturn(java.util.Optional.of(session));
        when(sessionRepository.findByIdAndUserId(100L, OTHER_USER)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.delete(100L, OTHER_USER)).isInstanceOf(ResourceNotFoundException.class);
        verify(sessionRepository, never()).delete(any());
        service.delete(100L, OWNER);
        verify(sessionRepository).delete(session);
    }
}
