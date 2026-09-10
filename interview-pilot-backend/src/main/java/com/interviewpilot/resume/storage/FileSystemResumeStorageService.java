package com.interviewpilot.resume.storage;

import com.interviewpilot.exception.StorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemResumeStorageService implements ResumeStorageService {

    private final Path rootLocation;

    public FileSystemResumeStorageService(@Value("${resume.storage.path:uploads/resume}") String storagePath) {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException exception) {
            throw new StorageException("Unable to initialize storage directory: " + exception.getMessage());
        }
    }

    @Override
    public StoredFile store(MultipartFile file) {
        try {
            String originalFileName = Path.of(file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename()).getFileName().toString();
            String extension = extractExtension(originalFileName);
            String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;
            String relativePath = buildRelativePath(extension, uniqueFileName);
            Path targetPath = rootLocation.resolve(relativePath).normalize();
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath);

            return StoredFile.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(uniqueFileName)
                    .storagePath(targetPath.toString())
                    .fileExtension(extension)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();
        } catch (IOException exception) {
            throw new StorageException("Failed to store resume file: " + exception.getMessage());
        }
    }

    private String buildRelativePath(String extension, String fileName) {
        LocalDateTime now = LocalDateTime.now();
        return now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + fileName;
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
