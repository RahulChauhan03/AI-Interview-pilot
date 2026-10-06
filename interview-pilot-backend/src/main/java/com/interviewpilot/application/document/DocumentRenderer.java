package com.interviewpilot.application.document;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/** Renders the stored documents to PDF (on demand; nothing is written to disk). */
@Component
public class DocumentRenderer {

    public byte[] resume(TailoredResumeContent resume) {
        try (PdfWriter pdf = new PdfWriter(resume.name() + " - Resume", resume.name())) {
            pdf.heading(resume.name(), 20);
            pdf.space(2);
            pdf.muted(String.join("  |  ", resume.contact()), 9.5f);
            if (!resume.summary().isBlank()) {
                pdf.section("Summary");
                pdf.text(resume.summary(), 10);
            }
            if (!resume.skills().isEmpty()) {
                pdf.section("Skills");
                pdf.text(String.join(", ", resume.skills()), 10);
            }
            if (!resume.experience().isEmpty()) {
                pdf.section("Experience");
                for (TailoredResumeContent.Experience job : resume.experience()) {
                    pdf.boldText(join(" - ", job.title(), job.company()), 10.5f);
                    pdf.muted(join("  |  ", job.duration(), job.location()), 9);
                    pdf.space(2);
                    job.bullets().forEach(bullet -> pdf.bullet(bullet, 10));
                    pdf.space(6);
                }
            }
            if (!resume.projects().isEmpty()) {
                pdf.section("Projects");
                for (TailoredResumeContent.Project project : resume.projects()) {
                    pdf.boldText(project.name(), 10.5f);
                    pdf.text(project.description(), 10);
                    if (!project.technologies().isEmpty()) pdf.muted("Technologies: " + String.join(", ", project.technologies()), 9);
                    pdf.space(6);
                }
            }
            if (!resume.education().isEmpty()) {
                pdf.section("Education");
                for (TailoredResumeContent.Education education : resume.education()) {
                    pdf.boldText(education.degree().isBlank() ? education.institution() : education.degree(), 10.5f);
                    pdf.muted(join("  |  ", education.degree().isBlank() ? "" : education.institution(), education.duration(), education.details()), 9);
                    pdf.space(4);
                }
            }
            if (!resume.certifications().isEmpty()) {
                pdf.section("Certifications");
                resume.certifications().forEach(certification -> pdf.bullet(certification, 10));
            }
            return pdf.toBytes();
        }
    }

    public byte[] coverLetter(CoverLetterContent letter) {
        try (PdfWriter pdf = new PdfWriter(letter.senderName() + " - Cover Letter", letter.senderName())) {
            pdf.heading(letter.senderName(), 16);
            pdf.muted(String.join("  |  ", letter.senderContact()), 9.5f);
            pdf.space(22);
            pdf.text(letter.date(), 10.5f);
            pdf.space(12);
            pdf.text(letter.recipient(), 10.5f);
            pdf.space(12);
            pdf.boldText(letter.subject(), 10.5f);
            pdf.space(12);
            pdf.text(letter.greeting(), 10.5f);
            pdf.space(8);
            for (String paragraph : letter.paragraphs()) {
                pdf.text(paragraph, 10.5f);
                pdf.space(9);
            }
            pdf.text(letter.closing(), 10.5f);
            pdf.space(18);
            pdf.text(letter.senderName(), 10.5f);
            return pdf.toBytes();
        }
    }

    public byte[] jobDescription(String jobTitle, String companyName, String description) {
        try (PdfWriter pdf = new PdfWriter(jobTitle + " - " + companyName, companyName)) {
            pdf.heading(jobTitle, 16);
            pdf.muted(companyName, 10.5f);
            pdf.section("Job description");
            for (String paragraph : description.split("\\R")) {
                if (paragraph.isBlank()) {
                    pdf.space(6);
                } else if (paragraph.strip().matches("^[-*\\u2022].*")) {
                    pdf.bullet(paragraph.strip().replaceFirst("^[-*\\u2022]\\s*", ""), 10);
                } else {
                    pdf.text(paragraph, 10);
                }
            }
            return pdf.toBytes();
        }
    }

    /** "Asha Rao", "Acme Fintech", "Backend Engineer", "Resume" -> "Asha_Rao_Acme_Fintech_Backend_Engineer_Resume". */
    public static String fileName(String... parts) {
        String name = String.join("_", Arrays.stream(parts)
                .map(part -> part == null ? "" : part.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", ""))
                .filter(part -> !part.isEmpty())
                .toList());
        return name.length() > 120 ? name.substring(0, 120) : name;
    }

    private static String join(String separator, String... parts) {
        return String.join(separator, Arrays.stream(parts).filter(part -> part != null && !part.isBlank()).toList());
    }
}
