package com.interviewpilot.resume.parser;

import com.interviewpilot.exception.ResumeParsingException;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

@Service
public class TikaResumeParserService implements ResumeParserService {

    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[ \\t]+");
    private static final Pattern INVISIBLE_CHARACTERS = Pattern.compile("[\\p{C}&&[^\\n\\r\\t]]");
    private final Tika tika = new Tika();

    @Override
    public String extractText(Path filePath) {
        try {
            String text = tika.parseToString(filePath);
            if (text == null || text.isBlank()) {
                throw new ResumeParsingException("No readable text was found in the resume");
            }
            return text;
        } catch (Exception exception) {
            if (exception instanceof ResumeParsingException parsingException) {
                throw parsingException;
            }
            throw new ResumeParsingException("Failed to extract text from resume: " + exception.getMessage());
        }
    }

    @Override
    public String cleanText(String rawText) {
        if (rawText == null || rawText.isBlank()) return "";
        String normalized = rawText.replace("\r\n", "\n").replace('\r', '\n').replace('\u00a0', ' ');
        normalized = INVISIBLE_CHARACTERS.matcher(normalized).replaceAll("");
        return normalized.lines().map(line -> MULTIPLE_SPACES.matcher(line).replaceAll(" ").trim())
                .filter(line -> !line.isEmpty()).reduce((left, right) -> left + "\n" + right).orElse("");
    }
}
