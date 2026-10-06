package com.interviewpilot.application.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class DocumentRendererTest {

    private final DocumentRenderer renderer = new DocumentRenderer();

    private static TailoredResumeContent resume(List<String> bullets) {
        return new TailoredResumeContent("Asha Rao", List.of("asha@example.com", "Pune"),
                "Backend developer with 3 years of experience in Java.", List.of("Java", "Spring Boot"),
                List.of(new TailoredResumeContent.Experience("Software Engineer", "Infosys", "Jun 2021 - Present", "Pune", bullets)),
                List.of(new TailoredResumeContent.Project("Interview Pilot", "AI interview practice app", List.of("Angular"))),
                List.of(new TailoredResumeContent.Education("B.E., Computer Engineering", "Pune University", "2017 - 2021", "")),
                List.of("Oracle Certified Java Programmer"));
    }

    @Test
    void resumePdfContainsTheResumeAndNoInternalData() throws IOException {
        byte[] pdf = renderer.resume(resume(List.of("Built REST APIs with Spring Boot")));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Asha Rao", "asha@example.com", "SUMMARY", "SKILLS", "Java, Spring Boot",
                    "Software Engineer - Infosys", "Jun 2021 - Present", "Built REST APIs with Spring Boot",
                    "Interview Pilot", "Pune University", "Oracle Certified Java Programmer");
            assertThat(text).doesNotContain("{", "\"", "null");
            assertThat(document.getDocumentInformation().getTitle()).contains("Asha Rao");
        }
    }

    @Test
    void charactersTheFontCannotDrawAreReplacedInsteadOfFailing() throws IOException {
        byte[] pdf = renderer.resume(resume(List.of("Shipped “smart” features — fast → 🚀 中文")));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Shipped \"smart\" features - fast");
        }
    }

    @Test
    void longContentContinuesOnANewPage() throws IOException {
        byte[] pdf = renderer.resume(resume(Collections.nCopies(80, "Built and maintained REST APIs with Spring Boot and MySQL for internal teams")));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    void coverLetterPdfUsesTheLetterLayout() throws IOException {
        CoverLetterContent letter = new CoverLetterContent("Asha Rao", List.of("asha@example.com"), "October 6, 2026",
                "Hiring Team, Acme Fintech", "Application for Backend Engineer", "Dear Hiring Manager,",
                List.of("First paragraph.", "Second paragraph."), "Sincerely,");

        try (PDDocument document = Loader.loadPDF(renderer.coverLetter(letter))) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Asha Rao", "October 6, 2026", "Hiring Team, Acme Fintech", "Application for Backend Engineer",
                    "Dear Hiring Manager,", "First paragraph.", "Second paragraph.", "Sincerely,");
        }
    }

    @Test
    void fileNamesOnlyContainSafeCharacters() {
        assertThat(DocumentRenderer.fileName("Asha Rao", "Acme Fintech", "Backend Engineer", "Resume"))
                .isEqualTo("Asha_Rao_Acme_Fintech_Backend_Engineer_Resume");
        assertThat(DocumentRenderer.fileName("../../etc/passwd", "Acme, Inc.", "C++ Dev\r\n", null, "Resume"))
                .isEqualTo("etc_passwd_Acme_Inc_C_Dev_Resume");
        assertThat(DocumentRenderer.fileName("x".repeat(300))).hasSize(120);
    }
}
