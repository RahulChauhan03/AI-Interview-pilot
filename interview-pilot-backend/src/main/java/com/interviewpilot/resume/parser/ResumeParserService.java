package com.interviewpilot.resume.parser;

import java.nio.file.Path;

public interface ResumeParserService {

    String extractText(Path filePath);

    String cleanText(String rawText);
}
