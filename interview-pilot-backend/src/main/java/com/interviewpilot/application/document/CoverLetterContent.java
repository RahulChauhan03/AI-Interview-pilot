package com.interviewpilot.application.document;

import java.util.List;

/** A cover letter as stored and rendered. Only the paragraphs come from the AI (and may be edited by the user). */
public record CoverLetterContent(
        String senderName,
        List<String> senderContact,
        String date,
        String recipient,
        String subject,
        String greeting,
        List<String> paragraphs,
        String closing) {
}
