package com.interviewpilot.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.application.ApplicationTestData;
import com.interviewpilot.application.document.CoverLetterContent;
import com.interviewpilot.application.document.DocumentRenderer;
import com.interviewpilot.application.document.DownloadFile;
import com.interviewpilot.application.document.StoredDocuments;
import com.interviewpilot.application.document.TailoredResumeContent;
import com.interviewpilot.application.dto.CoverLetterDto;
import com.interviewpilot.application.dto.TailoredResumeDto;
import com.interviewpilot.application.entity.ApplicationDocument;
import com.interviewpilot.application.entity.DocumentType;
import com.interviewpilot.application.entity.JobApplication;
import com.interviewpilot.application.repository.ApplicationDocumentRepository;
import com.interviewpilot.application.service.ApplicationService;
import com.interviewpilot.exception.ConflictException;
import com.interviewpilot.exception.InvalidAiResponseException;
import com.interviewpilot.exception.OllamaUnavailableException;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.resume.ai.AiResponseParser;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.service.ResumeService;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationDocumentServiceImplTest {

    private static final long OWNER = 1L;
    private static final long OTHER_USER = 2L;
    private static final long APPLICATION_ID = 7L;
    private static final long RESUME_ID = 5L;

    @Mock private ApplicationDocumentRepository documentRepository;
    @Mock private ApplicationService applicationService;
    @Mock private JobDescriptionService jobDescriptionService;
    @Mock private ResumeService resumeService;
    @Mock private UserRepository userRepository;
    @Mock private OllamaService ollamaService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ApplicationDocumentServiceImpl service;
    private JobApplication application;
    private JobDescription job;
    private final Resume resume = Resume.builder().id(RESUME_ID).originalFileName("asha-resume.pdf").build();

    @BeforeEach
    void setUp() {
        service = new ApplicationDocumentServiceImpl(documentRepository, applicationService, jobDescriptionService, resumeService,
                userRepository, ollamaService, new AiResponseParser(objectMapper), new DocumentRenderer(), objectMapper);
        job = JobDescription.builder().id(10L).companyName("Acme Fintech").jobTitle("AWS Backend Engineer")
                .jobDescription("Join the Payments team. Build Java and Spring Boot services on AWS. Kafka is a plus.").build();
        application = JobApplication.builder().id(APPLICATION_ID).jobDescription(job).status("SAVED").build();
        lenient().when(applicationService.findOwned(APPLICATION_ID, OWNER)).thenReturn(application);
        lenient().when(jobDescriptionService.findOwned(10L, OWNER)).thenReturn(job);
        lenient().when(userRepository.findById(OWNER)).thenReturn(Optional.of(User.builder().id(OWNER).firstName("Asha").lastName("Rao").build()));
        lenient().when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void resumeAndMatch(ParsedResume parsed) {
        when(resumeService.findParsedByIdForUser(RESUME_ID, OWNER)).thenReturn(parsed);
        when(resumeService.findByIdForUser(RESUME_ID, OWNER)).thenReturn(resume);
        when(jobDescriptionService.matchResume(10L, RESUME_ID, OWNER)).thenReturn(ResumeMatchResponseDto.builder()
                .id(3L).resumeId(RESUME_ID).matchScore(62.0).missingSkills(List.of("AWS", "Kafka")).build());
    }

    private String savedContent() {
        ArgumentCaptor<ApplicationDocument> saved = ArgumentCaptor.forClass(ApplicationDocument.class);
        verify(documentRepository).save(saved.capture());
        return saved.getValue().getContent();
    }

    // ---------------------------------------------------------------- tailored resume

    @Test
    void tailoredResumeKeepsResumeFactsAndDropsInventedClaims() throws Exception {
        resumeAndMatch(ApplicationTestData.parsedResume());
        when(ollamaService.generateJson(anyString(), anyMap())).thenReturn("""
                {"summary": "Backend developer with 5 years of AWS experience.",
                 "skills": ["Spring Boot", "Kubernetes", "java", "AWS", ""],
                 "experience": [
                   {"index": 0, "bullets": ["Designed REST APIs with Spring Boot and MySQL for insurance claims",
                                            "Improved throughput by 45%", "Deployed the platform on AWS",
                                            "Cut report generation time by 30% with caching"]},
                   {"index": 4, "bullets": ["Founded a startup"]}],
                 "projects": [{"index": 0, "description": "Built an AI interview practice app with Angular and Spring Boot."}]}
                """);

        TailoredResumeDto result = service.generateTailoredResume(APPLICATION_ID, RESUME_ID, OWNER);

        TailoredResumeContent content = result.getContent();
        assertThat(content.name()).isEqualTo("Asha Rao");
        assertThat(content.summary()).isEqualTo("Backend developer with 3 years of experience in Java and Spring Boot.");
        assertThat(content.skills()).containsExactly("Spring Boot", "Java", "MySQL", "Angular", "JUnit", "Mockito");
        assertThat(content.experience()).singleElement().satisfies(job -> {
            assertThat(job.title()).isEqualTo("Software Engineer");
            assertThat(job.company()).isEqualTo("Infosys");
            assertThat(job.duration()).isEqualTo("Jun 2021 - Present");
            assertThat(job.bullets()).containsExactly("Designed REST APIs with Spring Boot and MySQL for insurance claims",
                    "Cut report generation time by 30% with caching");
        });
        assertThat(content.projects()).singleElement().satisfies(project -> {
            assertThat(project.name()).isEqualTo("Interview Pilot");
            assertThat(project.description()).isEqualTo("Built an AI interview practice app with Angular and Spring Boot.");
        });
        assertThat(content.education()).singleElement().satisfies(education -> assertThat(education.institution()).isEqualTo("Pune University"));
        assertThat(content.certifications()).containsExactly("Oracle Certified Java Programmer");
        // summary, Kubernetes, AWS, two bullets
        assertThat(result.getRemovedSuggestions()).isEqualTo(5);
        assertThat(result.getOmittedSkills()).containsExactly("AWS", "Kafka");
        assertThat(result.getBaseResumeFileName()).isEqualTo("asha-resume.pdf");
        assertThat(savedContent()).doesNotContain("Kubernetes", "45%", "Founded");
        verify(applicationService).recordPreparation(APPLICATION_ID, RESUME_ID, OWNER);
    }

    @Test
    void promptListsTheResumeByIndexAndTheSkillsToAvoid() {
        resumeAndMatch(ApplicationTestData.parsedResume());
        when(ollamaService.generateJson(anyString(), anyMap())).thenReturn(
                "{\"summary\": \"\", \"skills\": [], \"experience\": [], \"projects\": []}");

        TailoredResumeDto result = service.generateTailoredResume(APPLICATION_ID, RESUME_ID, OWNER);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ollamaService).generateJson(prompt.capture(), anyMap());
        assertThat(prompt.getValue()).contains("[0] Software Engineer at Infosys", "[0] Interview Pilot",
                "does NOT have these, so never mention them: AWS, Kafka", "JOB: AWS Backend Engineer at Acme Fintech");
        // Nothing usable from the AI: the original resume is kept unchanged.
        assertThat(result.getContent().experience().get(0).bullets()).hasSize(3);
        assertThat(result.getRemovedSuggestions()).isZero();
    }

    @Test
    void invalidAiOutputIsRejectedAndNothingIsSaved() {
        resumeAndMatch(ApplicationTestData.parsedResume());
        when(ollamaService.generateJson(anyString(), anyMap())).thenReturn("{\"summary\": \"Only a summary\"}");

        assertThatThrownBy(() -> service.generateTailoredResume(APPLICATION_ID, RESUME_ID, OWNER))
                .isInstanceOf(InvalidAiResponseException.class);
        verify(documentRepository, never()).save(any());
        verify(applicationService, never()).recordPreparation(any(), any(), any());
    }

    @Test
    void aResumeMustBeChosenFirst() {
        assertThatThrownBy(() -> service.generateTailoredResume(APPLICATION_ID, null, OWNER))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(ollamaService);
    }

    @Test
    void aResumeWithoutExperienceOrProjectsFailsBeforeTheSlowMatchAnalysis() {
        when(resumeService.findParsedByIdForUser(RESUME_ID, OWNER)).thenReturn(ParsedResume.builder().cleanText("Asha Rao").skills(List.of("Java")).build());
        when(resumeService.findByIdForUser(RESUME_ID, OWNER)).thenReturn(resume);

        assertThatThrownBy(() -> service.generateTailoredResume(APPLICATION_ID, RESUME_ID, OWNER))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Upload it again");
        verify(jobDescriptionService, never()).matchResume(any(), any(), any());
        verifyNoInteractions(ollamaService);
    }

    @Test
    void anotherUsersApplicationIsNotFound() {
        when(applicationService.findOwned(APPLICATION_ID, OTHER_USER)).thenThrow(new ResourceNotFoundException("Application not found"));

        assertThatThrownBy(() -> service.generateTailoredResume(APPLICATION_ID, RESUME_ID, OTHER_USER))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.tailoredResumePdf(APPLICATION_ID, OTHER_USER)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.coverLetterPdf(APPLICATION_ID, OTHER_USER)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.applicationPackage(APPLICATION_ID, OTHER_USER)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(ollamaService, resumeService, documentRepository);
    }

    // ---------------------------------------------------------------- cover letter

    @Test
    void coverLetterUsesAFixedHeaderAndRemovesUnsupportedSentences() {
        resumeAndMatch(ApplicationTestData.parsedResume());
        when(ollamaService.generateJson(anyString(), anyMap())).thenReturn("""
                {"paragraphs": [
                  "Dear Sarah Johnson,",
                  "Dear Hiring Manager, I am applying for the AWS Backend Engineer role at Acme Fintech. I have 3 years of experience with Java and Spring Boot.",
                  "At Infosys I built REST APIs with Spring Boot and MySQL. I also led a team of 12 engineers on AWS. I would like to bring this work to the Payments team.",
                  "I would welcome the chance to discuss how my backend work fits [Company]'s needs.",
                  "I also built data pipelines at Google. My project Interview Pilot uses Angular and Spring Boot.",
                  "Sincerely, Asha Rao"]}
                """);

        CoverLetterDto result = service.generateCoverLetter(APPLICATION_ID, RESUME_ID, OWNER);

        CoverLetterContent letter = result.getContent();
        assertThat(letter.greeting()).isEqualTo("Dear Hiring Manager,");
        assertThat(letter.recipient()).isEqualTo("Hiring Team, Acme Fintech");
        assertThat(letter.subject()).isEqualTo("Application for AWS Backend Engineer");
        assertThat(letter.closing()).isEqualTo("Sincerely,");
        assertThat(letter.senderName()).isEqualTo("Asha Rao");
        assertThat(letter.paragraphs()).containsExactly(
                "I am applying for the AWS Backend Engineer role at Acme Fintech. I have 3 years of experience with Java and Spring Boot.",
                "At Infosys I built REST APIs with Spring Boot and MySQL. I would like to bring this work to the Payments team.",
                "My project Interview Pilot uses Angular and Spring Boot.");
        // 12 engineers on AWS, the [Company] placeholder sentence, the invented employer
        assertThat(result.getRemovedSentences()).isEqualTo(3);
        assertThat(result.isEdited()).isFalse();
        assertThat(savedContent()).doesNotContain("Sarah", "12 engineers", "Google", "[Company]");
        verify(applicationService).recordPreparation(APPLICATION_ID, RESUME_ID, OWNER);
    }

    @Test
    void coverLetterWithoutEnoughSupportedTextIsRejected() {
        resumeAndMatch(ApplicationTestData.parsedResume());
        when(ollamaService.generateJson(anyString(), anyMap())).thenReturn("""
                {"paragraphs": ["I have 10 years of AWS experience.", "I built Kafka pipelines at Google.", "Regards"]}
                """);

        assertThatThrownBy(() -> service.generateCoverLetter(APPLICATION_ID, RESUME_ID, OWNER))
                .isInstanceOf(InvalidAiResponseException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void ollamaFailurePropagatesAndNothingIsSaved() {
        resumeAndMatch(ApplicationTestData.parsedResume());
        when(ollamaService.generateJson(anyString(), anyMap())).thenThrow(new OllamaUnavailableException("Ollama is unreachable", null));

        assertThatThrownBy(() -> service.generateCoverLetter(APPLICATION_ID, RESUME_ID, OWNER))
                .isInstanceOf(OllamaUnavailableException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void editedCoverLetterKeepsTheUsersTextAndHeader() throws Exception {
        CoverLetterContent letter = new CoverLetterContent("Asha Rao", List.of("asha@example.com"), "October 6, 2026",
                "Hiring Team, Acme Fintech", "Application for AWS Backend Engineer", "Dear Hiring Manager,", List.of("Old."), "Sincerely,");
        ApplicationDocument document = ApplicationDocument.builder().id(1L).application(application).baseResume(resume)
                .documentType("COVER_LETTER").content(objectMapper.writeValueAsString(new StoredDocuments.CoverLetter(letter, 2, false))).build();
        when(documentRepository.findByApplicationIdAndDocumentType(APPLICATION_ID, "COVER_LETTER")).thenReturn(Optional.of(document));

        CoverLetterDto result = service.updateCoverLetter(APPLICATION_ID, List.of("  My own first paragraph. ", "", "Second."), OWNER);

        assertThat(result.getContent().paragraphs()).containsExactly("My own first paragraph.", "Second.");
        assertThat(result.getContent().greeting()).isEqualTo("Dear Hiring Manager,");
        assertThat(result.isEdited()).isTrue();
        assertThat(result.getRemovedSentences()).isEqualTo(2);
        assertThat(document.getContent()).contains("My own first paragraph.");
    }

    // ---------------------------------------------------------------- downloads

    private ApplicationDocument storedResume() throws Exception {
        TailoredResumeContent content = new TailoredResumeContent("Asha Rao", List.of("asha@example.com"), "Backend developer.",
                List.of("Java"), List.of(new TailoredResumeContent.Experience("Software Engineer", "Infosys", "2021 - Present", "", List.of("Built APIs"))),
                List.of(), List.of(), List.of());
        return ApplicationDocument.builder().id(2L).application(application).documentType(DocumentType.TAILORED_RESUME.name())
                .content(objectMapper.writeValueAsString(new StoredDocuments.TailoredResume(content, List.of("AWS"), 0))).build();
    }

    @Test
    void tailoredResumePdfHasADescriptiveSafeFileName() throws Exception {
        when(documentRepository.findByApplicationIdAndDocumentType(APPLICATION_ID, "TAILORED_RESUME")).thenReturn(Optional.of(storedResume()));

        DownloadFile file = service.tailoredResumePdf(APPLICATION_ID, OWNER);

        assertThat(file.fileName()).isEqualTo("Asha_Rao_Acme_Fintech_AWS_Backend_Engineer_Resume.pdf");
        assertThat(file.contentType()).isEqualTo("application/pdf");
        assertThat(new String(file.bytes(), 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void missingDocumentIsNotFound() {
        when(documentRepository.findByApplicationIdAndDocumentType(APPLICATION_ID, "COVER_LETTER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.coverLetterPdf(APPLICATION_ID, OWNER)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void packageContainsOnlyThePdfs() throws Exception {
        when(documentRepository.findByApplicationIdAndDocumentType(APPLICATION_ID, "TAILORED_RESUME")).thenReturn(Optional.of(storedResume()));
        when(documentRepository.findByApplicationIdAndDocumentType(APPLICATION_ID, "COVER_LETTER")).thenReturn(Optional.empty());

        DownloadFile file = service.applicationPackage(APPLICATION_ID, OWNER);

        assertThat(file.fileName()).isEqualTo("Asha_Rao_Acme_Fintech_Application.zip");
        assertThat(file.contentType()).isEqualTo("application/zip");
        assertThat(zipEntries(file.bytes())).containsExactly("Asha_Rao_Resume.pdf", "Job_Description.pdf");
    }

    @Test
    void packageNeedsAtLeastOneDocument() {
        when(documentRepository.findByApplicationIdAndDocumentType(eq(APPLICATION_ID), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applicationPackage(APPLICATION_ID, OWNER)).isInstanceOf(ConflictException.class);
    }

    private static List<String> zipEntries(byte[] zip) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                byte[] content = input.readAllBytes();
                assertThat(new String(content, 0, 5)).isEqualTo("%PDF-");
                names.add(entry.getName());
            }
        }
        return names;
    }
}
