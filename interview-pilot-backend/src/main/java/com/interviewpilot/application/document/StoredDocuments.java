package com.interviewpilot.application.document;

import java.util.List;

/** JSON stored in application_documents.content: the document plus how it was checked. */
public final class StoredDocuments {

    private StoredDocuments() {
    }

    public record TailoredResume(TailoredResumeContent resume, List<String> omittedSkills, int removedSuggestions) {
    }

    public record CoverLetter(CoverLetterContent letter, int removedSentences, boolean edited) {
    }
}
