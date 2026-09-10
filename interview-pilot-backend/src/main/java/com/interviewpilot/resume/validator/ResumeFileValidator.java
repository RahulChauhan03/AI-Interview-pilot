package com.interviewpilot.resume.validator;

import com.interviewpilot.exception.FileTooLargeException;
import com.interviewpilot.exception.UnsupportedFileException;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ResumeFileValidator {
    private static final Set<String> EXTENSIONS = Set.of("pdf", "doc", "docx");
    private final long maxFileSize;

    public ResumeFileValidator(@Value("${resume.max-file-size:10MB}") org.springframework.util.unit.DataSize maxFileSize) {
        this.maxFileSize = maxFileSize.toBytes();
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new UnsupportedFileException("A resume file is required");
        if (file.getSize() > maxFileSize) throw new FileTooLargeException("Resume file must not exceed 10 MB");
        String name = file.getOriginalFilename();
        int dot = name == null ? -1 : name.lastIndexOf('.');
        String extension = dot < 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(extension)) throw new UnsupportedFileException("Only PDF, DOC, and DOCX files are supported");
    }
}
