package com.interviewpilot.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.application.document.ClaimGuard;
import com.interviewpilot.application.document.CoverLetterContent;
import com.interviewpilot.application.document.DocumentRenderer;
import com.interviewpilot.application.document.DownloadFile;
import com.interviewpilot.application.document.ResumeFacts;
import com.interviewpilot.application.document.StoredDocuments;
import com.interviewpilot.application.document.TailoredResumeContent;
import com.interviewpilot.application.dto.CoverLetterDto;
import com.interviewpilot.application.dto.TailoredResumeDto;
import com.interviewpilot.application.entity.ApplicationDocument;
import com.interviewpilot.application.entity.DocumentType;
import com.interviewpilot.application.entity.JobApplication;
import com.interviewpilot.application.repository.ApplicationDocumentRepository;
import com.interviewpilot.application.service.ApplicationDocumentService;
import com.interviewpilot.application.service.ApplicationService;
import com.interviewpilot.exception.ConflictException;
import com.interviewpilot.exception.InvalidAiResponseException;
import com.interviewpilot.exception.ResourceNotFoundException;
import com.interviewpilot.jobdescription.dto.ResumeMatchResponseDto;
import com.interviewpilot.jobdescription.entity.JobDescription;
import com.interviewpilot.jobdescription.service.JobDescriptionService;
import com.interviewpilot.resume.ai.AiResponseParser;
import com.interviewpilot.resume.ai.CoverLetterDraft;
import com.interviewpilot.resume.ai.JsonSchemas;
import com.interviewpilot.resume.ai.OllamaService;
import com.interviewpilot.resume.ai.PromptText;
import com.interviewpilot.resume.ai.TailoredResumeDraft;
import com.interviewpilot.resume.document.ParsedResume;
import com.interviewpilot.resume.entity.Resume;
import com.interviewpilot.resume.service.ResumeService;
import com.interviewpilot.user.entity.User;
import com.interviewpilot.user.repository.UserRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates application documents with the AI and makes sure they stay truthful: the AI only rewrites wording,
 * every fact (companies, titles, dates, education, contact details) comes from the parsed resume, and any AI text
 * that the resume does not support is dropped (see {@link ClaimGuard}). AI calls run outside transactions.
 */
@Slf4j
@Service
public class ApplicationDocumentServiceImpl implements ApplicationDocumentService {

    private static final int MAX_BULLETS = 6;
    private static final Pattern GREETING = Pattern.compile("(?i)^dear\\b[^,.!:]{0,60}[,.!:]?\\s*");
    private static final Pattern SIGN_OFF = Pattern.compile("(?i)^(sincerely|best regards|kind regards|warm regards|regards|best"
            + "|thank you|thanks|yours (sincerely|truly|faithfully))\\b[^.!?]{0,40}[.!?]?$");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\[[^\\]]*\\]");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");
    private static final String PDF = "application/pdf";

    private final ApplicationDocumentRepository documentRepository;
    private final ApplicationService applicationService;
    private final JobDescriptionService jobDescriptionService;
    private final ResumeService resumeService;
    private final UserRepository userRepository;
    private final OllamaService ollamaService;
    private final AiResponseParser aiResponseParser;
    private final DocumentRenderer renderer;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> tailoredResumeSchema;
    private final Map<String, Object> coverLetterSchema;

    public ApplicationDocumentServiceImpl(ApplicationDocumentRepository documentRepository, ApplicationService applicationService,
                                          JobDescriptionService jobDescriptionService, ResumeService resumeService,
                                          UserRepository userRepository, OllamaService ollamaService, AiResponseParser aiResponseParser,
                                          DocumentRenderer renderer, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.applicationService = applicationService;
        this.jobDescriptionService = jobDescriptionService;
        this.resumeService = resumeService;
        this.userRepository = userRepository;
        this.ollamaService = ollamaService;
        this.aiResponseParser = aiResponseParser;
        this.renderer = renderer;
        this.objectMapper = objectMapper;
        this.tailoredResumeSchema = JsonSchemas.load(objectMapper, "ollama/tailored-resume-schema.json");
        this.coverLetterSchema = JsonSchemas.load(objectMapper, "ollama/cover-letter-schema.json");
    }

    // ---------------------------------------------------------------- tailored resume

    @Override
    public TailoredResumeDto generateTailoredResume(Long applicationId, Long resumeId, Long userId) {
        Context context = context(applicationId, resumeId, userId, true);
        ResumeFacts facts = context.facts();
        ClaimGuard guard = new ClaimGuard(facts.sourceText(), context.match().getMissingSkills());
        String prompt = tailoredResumePrompt(context.job(), facts, context.match().getMissingSkills());
        TailoredResumeDraft draft = aiResponseParser.parse(ollamaService.generateJson(prompt, tailoredResumeSchema), TailoredResumeDraft.class);

        int[] removed = {0};
        TailoredResumeContent content = assembleResume(facts, draft, guard, removed);
        StoredDocuments.TailoredResume stored = new StoredDocuments.TailoredResume(content, context.match().getMissingSkills(), removed[0]);
        ApplicationDocument document = save(context.application(), DocumentType.TAILORED_RESUME, context.resume(), stored);
        applicationService.recordPreparation(applicationId, context.resume().getId(), userId);
        log.info("Tailored resume generated: applicationId={}, removedSuggestions={}", applicationId, removed[0]);
        return toTailoredDto(applicationId, document, context.resume(), stored);
    }

    @Override
    @Transactional(readOnly = true)
    public TailoredResumeDto getTailoredResume(Long applicationId, Long userId) {
        applicationService.findOwned(applicationId, userId);
        ApplicationDocument document = document(applicationId, DocumentType.TAILORED_RESUME, "No tailored resume yet");
        return toTailoredDto(applicationId, document, document.getBaseResume(), read(document, StoredDocuments.TailoredResume.class));
    }

    /** Facts from the resume; only wording, order and selection from the AI. {@code removed[0]} counts dropped suggestions. */
    TailoredResumeContent assembleResume(ResumeFacts facts, TailoredResumeDraft draft, ClaimGuard guard, int[] removed) {
        String summary = facts.summary();
        if (!draft.summary().isBlank()) {
            if (guard.isSupported(draft.summary())) summary = draft.summary().trim();
            else removed[0]++;
        }

        Map<String, String> skills = new LinkedHashMap<>();
        for (String skill : draft.skills()) {
            if (skill.isBlank()) continue;
            Optional<String> own = facts.skills().stream().filter(item -> item.equalsIgnoreCase(skill.trim())).findFirst();
            if (own.isPresent()) skills.putIfAbsent(own.get().toLowerCase(Locale.ROOT), own.get());
            else if (facts.hasSkill(skill) && guard.isSupported(skill)) skills.putIfAbsent(skill.trim().toLowerCase(Locale.ROOT), skill.trim());
            else removed[0]++;
        }
        facts.skills().forEach(skill -> skills.putIfAbsent(skill.toLowerCase(Locale.ROOT), skill)); // AI only reorders

        Map<Integer, List<String>> bullets = new HashMap<>();
        draft.experience().forEach(item -> bullets.putIfAbsent(item.index(), item.bullets()));
        List<TailoredResumeContent.Experience> experience = new ArrayList<>();
        for (int i = 0; i < facts.experience().size(); i++) {
            ResumeFacts.Job job = facts.experience().get(i);
            List<String> kept = keepSupported(bullets.getOrDefault(i, List.of()), guard, removed);
            experience.add(new TailoredResumeContent.Experience(job.title(), job.company(), job.duration(), job.location(),
                    (kept.isEmpty() ? job.bullets() : kept).stream().limit(MAX_BULLETS).toList()));
        }

        Map<Integer, String> descriptions = new HashMap<>();
        draft.projects().forEach(item -> descriptions.putIfAbsent(item.index(), item.description()));
        List<TailoredResumeContent.Project> projects = new ArrayList<>();
        for (int i = 0; i < facts.projects().size(); i++) {
            ResumeFacts.ProjectFact project = facts.projects().get(i);
            String rewritten = descriptions.getOrDefault(i, "").trim();
            String description = project.description();
            if (!rewritten.isEmpty()) {
                if (guard.isSupported(rewritten)) description = rewritten;
                else removed[0]++;
            }
            projects.add(new TailoredResumeContent.Project(project.name(), description, project.technologies()));
        }
        return new TailoredResumeContent(facts.name(), facts.contact(), summary, new ArrayList<>(skills.values()),
                experience, projects, facts.education(), facts.certifications());
    }

    private String tailoredResumePrompt(JobDescription job, ResumeFacts facts, List<String> missingSkills) {
        StringBuilder prompt = new StringBuilder()
                .append("You are an expert resume writer. Tailor the candidate's resume for the job below by rewriting its wording ")
                .append("and choosing what to emphasise. Never add facts.\nRules:\n")
                .append("- Use only facts from the CANDIDATE RESUME. Never add companies, projects, skills, tools, certifications, ")
                .append("numbers, metrics or years that it does not contain.\n");
        if (!missingSkills.isEmpty()) {
            prompt.append("- The candidate does NOT have these, so never mention them: ").append(String.join(", ", missingSkills)).append(".\n");
        }
        prompt.append("- summary: 2-3 sentences aimed at this job, based only on the candidate's real experience.\n")
                .append("- skills: the candidate's own skills from the list below, most relevant to this job first.\n")
                .append("- experience: for each job, by its index, 2-5 concise bullet points rewritten from that job's own ")
                .append("responsibilities, starting with a strong verb. Keep every fact unchanged.\n")
                .append("- projects: for each project, by its index, one sentence rewritten from that project's own description.\n\n")
                .append("JOB: ").append(job.getJobTitle()).append(" at ").append(job.getCompanyName()).append('\n')
                .append(PromptText.limit(job.getJobDescription(), 4000)).append("\n\n");
        appendCandidate(prompt, facts);
        return prompt.toString();
    }

    // ---------------------------------------------------------------- cover letter

    @Override
    public CoverLetterDto generateCoverLetter(Long applicationId, Long resumeId, Long userId) {
        Context context = context(applicationId, resumeId, userId, false);
        // The letter may name the role and company (even when a word of the title is a missing skill) and other
        // names from the job description; claims are still checked against the resume.
        JobDescription job = context.job();
        ClaimGuard guard = new ClaimGuard(context.facts().sourceText(), context.match().getMissingSkills(),
                List.of(job.getJobTitle(), job.getCompanyName()), job.getJobDescription());
        String prompt = coverLetterPrompt(context.job(), context.facts(), context.match().getMissingSkills());
        CoverLetterDraft draft = aiResponseParser.parse(ollamaService.generateJson(prompt, coverLetterSchema), CoverLetterDraft.class);

        int removed = 0;
        List<String> paragraphs = new ArrayList<>();
        for (String raw : draft.paragraphs()) {
            // Greeting and sign-off are fixed by us; sentences with unfilled placeholders such as [Company] are dropped.
            String paragraph = GREETING.matcher(raw == null ? "" : raw.replaceAll("\\s+", " ").trim()).replaceFirst("");
            if (paragraph.isEmpty() || SIGN_OFF.matcher(paragraph).matches()) continue;
            List<String> sentences = List.of(SENTENCE_END.split(paragraph));
            String kept = guard.keepSupportedSentences(String.join(" ",
                    sentences.stream().filter(sentence -> !PLACEHOLDER.matcher(sentence).find()).toList()));
            removed += sentences.size() - sentenceCount(kept);
            if (!kept.isBlank()) paragraphs.add(kept);
        }
        if (paragraphs.size() < 2) {
            throw new InvalidAiResponseException("AI cover letter was not supported by the resume");
        }
        CoverLetterContent letter = new CoverLetterContent(context.facts().name(), context.facts().contact().stream().limit(4).toList(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)),
                "Hiring Team, " + job.getCompanyName(), "Application for " + job.getJobTitle(), "Dear Hiring Manager,",
                paragraphs, "Sincerely,");
        StoredDocuments.CoverLetter stored = new StoredDocuments.CoverLetter(letter, removed, false);
        ApplicationDocument document = save(context.application(), DocumentType.COVER_LETTER, context.resume(), stored);
        applicationService.recordPreparation(applicationId, context.resume().getId(), userId);
        log.info("Cover letter generated: applicationId={}, removedSentences={}", applicationId, removed);
        return toCoverLetterDto(applicationId, document, context.resume(), stored);
    }

    @Override
    @Transactional(readOnly = true)
    public CoverLetterDto getCoverLetter(Long applicationId, Long userId) {
        applicationService.findOwned(applicationId, userId);
        ApplicationDocument document = document(applicationId, DocumentType.COVER_LETTER, "No cover letter yet");
        return toCoverLetterDto(applicationId, document, document.getBaseResume(), read(document, StoredDocuments.CoverLetter.class));
    }

    /** The user's own edits are kept as written. */
    @Override
    @Transactional
    public CoverLetterDto updateCoverLetter(Long applicationId, List<String> paragraphs, Long userId) {
        applicationService.findOwned(applicationId, userId);
        ApplicationDocument document = document(applicationId, DocumentType.COVER_LETTER, "No cover letter yet");
        StoredDocuments.CoverLetter current = read(document, StoredDocuments.CoverLetter.class);
        CoverLetterContent letter = current.letter();
        CoverLetterContent edited = new CoverLetterContent(letter.senderName(), letter.senderContact(), letter.date(), letter.recipient(),
                letter.subject(), letter.greeting(), paragraphs.stream().map(String::trim).filter(p -> !p.isEmpty()).toList(), letter.closing());
        StoredDocuments.CoverLetter stored = new StoredDocuments.CoverLetter(edited, current.removedSentences(), true);
        document.setContent(write(stored));
        documentRepository.save(document);
        return toCoverLetterDto(applicationId, document, document.getBaseResume(), stored);
    }

    private String coverLetterPrompt(JobDescription job, ResumeFacts facts, List<String> missingSkills) {
        StringBuilder prompt = new StringBuilder()
                .append("Write the body of a cover letter from the candidate below for the job below.\nRules:\n")
                .append("- 3 or 4 short paragraphs, about 250 words in total. Plain text only: no greeting, no sign-off, ")
                .append("no placeholders such as [Company].\n")
                .append("- Connect the candidate's real experience, projects and skills to what this job asks for. ")
                .append("Mention the company and the role.\n")
                .append("- Use only facts from the candidate's resume. Do not invent experience, achievements, numbers, ")
                .append("technologies, or anything about the company that is not in the job description.\n");
        if (!missingSkills.isEmpty()) {
            prompt.append("- The candidate does NOT have these, so do not mention them: ").append(String.join(", ", missingSkills)).append(".\n");
        }
        prompt.append("- Avoid cliches such as \"I am writing to express my interest\", \"passionate\", \"perfect fit\" and \"dynamic team\".\n\n")
                .append("JOB: ").append(job.getJobTitle()).append(" at ").append(job.getCompanyName()).append('\n')
                .append(PromptText.limit(job.getJobDescription(), 4000)).append("\n\n");
        appendCandidate(prompt, facts);
        return prompt.toString();
    }

    // ---------------------------------------------------------------- downloads

    @Override
    @Transactional(readOnly = true)
    public DownloadFile tailoredResumePdf(Long applicationId, Long userId) {
        JobApplication application = applicationService.findOwned(applicationId, userId);
        JobDescription job = jobDescriptionService.findOwned(application.getJobDescription().getId(), userId);
        TailoredResumeContent resume = read(document(applicationId, DocumentType.TAILORED_RESUME, "No tailored resume yet"),
                StoredDocuments.TailoredResume.class).resume();
        String name = DocumentRenderer.fileName(resume.name(), job.getCompanyName(), job.getJobTitle(), "Resume") + ".pdf";
        return new DownloadFile(name, PDF, renderer.resume(resume));
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadFile coverLetterPdf(Long applicationId, Long userId) {
        JobApplication application = applicationService.findOwned(applicationId, userId);
        JobDescription job = jobDescriptionService.findOwned(application.getJobDescription().getId(), userId);
        CoverLetterContent letter = read(document(applicationId, DocumentType.COVER_LETTER, "No cover letter yet"),
                StoredDocuments.CoverLetter.class).letter();
        String name = DocumentRenderer.fileName(letter.senderName(), job.getCompanyName(), job.getJobTitle(), "Cover_Letter") + ".pdf";
        return new DownloadFile(name, PDF, renderer.coverLetter(letter));
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadFile applicationPackage(Long applicationId, Long userId) {
        JobApplication application = applicationService.findOwned(applicationId, userId);
        JobDescription job = jobDescriptionService.findOwned(application.getJobDescription().getId(), userId);
        Optional<ApplicationDocument> resume = documentRepository.findByApplicationIdAndDocumentType(applicationId, DocumentType.TAILORED_RESUME.name());
        Optional<ApplicationDocument> letter = documentRepository.findByApplicationIdAndDocumentType(applicationId, DocumentType.COVER_LETTER.name());
        if (resume.isEmpty() && letter.isEmpty()) {
            throw new ConflictException("Generate a tailored resume or cover letter first");
        }
        String person = resume.map(document -> read(document, StoredDocuments.TailoredResume.class).resume().name())
                .or(() -> letter.map(document -> read(document, StoredDocuments.CoverLetter.class).letter().senderName()))
                .orElseGet(() -> accountName(userId));
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes)) {
            if (resume.isPresent()) {
                add(zip, DocumentRenderer.fileName(person, "Resume") + ".pdf",
                        renderer.resume(read(resume.get(), StoredDocuments.TailoredResume.class).resume()));
            }
            if (letter.isPresent()) {
                add(zip, DocumentRenderer.fileName(person, "Cover_Letter") + ".pdf",
                        renderer.coverLetter(read(letter.get(), StoredDocuments.CoverLetter.class).letter()));
            }
            add(zip, "Job_Description.pdf", renderer.jobDescription(job.getJobTitle(), job.getCompanyName(), job.getJobDescription()));
            zip.finish();
            String name = DocumentRenderer.fileName(person, job.getCompanyName(), "Application") + ".zip";
            return new DownloadFile(name, "application/zip", bytes.toByteArray());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    // ---------------------------------------------------------------- helpers

    private record Context(JobApplication application, JobDescription job, Resume resume, ResumeFacts facts, ResumeMatchResponseDto match) {
    }

    /**
     * Ownership checks, the base resume, and the match analysis (reused when nothing changed) before any generation.
     * {@code needsExperience}: fail before the slow match analysis when the resume has nothing to tailor.
     */
    private Context context(Long applicationId, Long requestedResumeId, Long userId, boolean needsExperience) {
        JobApplication application = applicationService.findOwned(applicationId, userId);
        JobDescription job = jobDescriptionService.findOwned(application.getJobDescription().getId(), userId);
        Long resumeId = requestedResumeId != null ? requestedResumeId
                : application.getResume() == null ? null : application.getResume().getId();
        if (resumeId == null) {
            throw new ConflictException("Choose the resume to use for this application");
        }
        ParsedResume parsed = resumeService.findParsedByIdForUser(resumeId, userId);
        Resume resume = resumeService.findByIdForUser(resumeId, userId);
        ResumeFacts facts = ResumeFacts.from(parsed, accountName(userId));
        if (needsExperience && facts.experience().isEmpty() && facts.projects().isEmpty()) {
            throw new ConflictException("This resume has no work experience or projects to tailor. Upload it again so it is re-analysed.");
        }
        ResumeMatchResponseDto match = jobDescriptionService.matchResume(job.getId(), resumeId, userId);
        return new Context(application, job, resume, facts, match);
    }

    private void appendCandidate(StringBuilder prompt, ResumeFacts facts) {
        prompt.append("CANDIDATE RESUME\n")
                .append("Name: ").append(facts.name()).append('\n')
                .append("Summary: ").append(facts.summary()).append('\n')
                .append("Skills: ").append(String.join(", ", facts.skills())).append('\n')
                .append("Experience:\n");
        for (int i = 0; i < facts.experience().size(); i++) {
            ResumeFacts.Job job = facts.experience().get(i);
            prompt.append('[').append(i).append("] ").append(job.title()).append(" at ").append(job.company())
                    .append(" (").append(job.duration()).append(")\n");
            job.bullets().forEach(bullet -> prompt.append("  - ").append(bullet).append('\n'));
            if (!job.technologies().isEmpty()) prompt.append("  Technologies: ").append(String.join(", ", job.technologies())).append('\n');
        }
        prompt.append("Projects:\n");
        for (int i = 0; i < facts.projects().size(); i++) {
            ResumeFacts.ProjectFact project = facts.projects().get(i);
            prompt.append('[').append(i).append("] ").append(project.name()).append(": ").append(project.description());
            if (!project.technologies().isEmpty()) prompt.append(" (Technologies: ").append(String.join(", ", project.technologies())).append(')');
            prompt.append('\n');
        }
    }

    private List<String> keepSupported(List<String> texts, ClaimGuard guard, int[] removed) {
        List<String> kept = new ArrayList<>();
        for (String text : texts) {
            if (text == null || text.isBlank()) continue;
            if (guard.isSupported(text)) kept.add(text.trim().replaceFirst("^[-*\\u2022]\\s*", ""));
            else removed[0]++;
        }
        return kept;
    }

    private static int sentenceCount(String text) {
        return text == null || text.isBlank() ? 0 : SENTENCE_END.split(text.trim()).length;
    }

    private ApplicationDocument save(JobApplication application, DocumentType type, Resume resume, Object stored) {
        ApplicationDocument document = documentRepository.findByApplicationIdAndDocumentType(application.getId(), type.name())
                .orElseGet(() -> ApplicationDocument.builder().application(application).documentType(type.name()).build());
        document.setBaseResume(resume);
        document.setContent(write(stored));
        return documentRepository.save(document);
    }

    private ApplicationDocument document(Long applicationId, DocumentType type, String notFoundMessage) {
        return documentRepository.findByApplicationIdAndDocumentType(applicationId, type.name())
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }

    private TailoredResumeDto toTailoredDto(Long applicationId, ApplicationDocument document, Resume baseResume,
                                            StoredDocuments.TailoredResume stored) {
        return TailoredResumeDto.builder()
                .applicationId(applicationId)
                .baseResumeId(baseResume == null ? null : baseResume.getId())
                .baseResumeFileName(baseResume == null ? null : baseResume.getOriginalFileName())
                .generatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt() : document.getCreatedAt())
                .content(stored.resume())
                .omittedSkills(stored.omittedSkills())
                .removedSuggestions(stored.removedSuggestions())
                .build();
    }

    private CoverLetterDto toCoverLetterDto(Long applicationId, ApplicationDocument document, Resume baseResume,
                                            StoredDocuments.CoverLetter stored) {
        return CoverLetterDto.builder()
                .applicationId(applicationId)
                .baseResumeId(baseResume == null ? null : baseResume.getId())
                .updatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt() : document.getCreatedAt())
                .content(stored.letter())
                .removedSentences(stored.removedSentences())
                .edited(stored.edited())
                .build();
    }

    private String accountName(Long userId) {
        return userRepository.findById(userId).map(this::fullName).orElse("Candidate");
    }

    private String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private static void add(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot store document", exception);
        }
    }

    private <T> T read(ApplicationDocument document, Class<T> type) {
        try {
            return objectMapper.readValue(document.getContent(), type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored document " + document.getId() + " cannot be read", exception);
        }
    }
}
