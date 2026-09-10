package com.interviewpilot.resume.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFile {

    private String originalFileName;
    private String storedFileName;
    private String storagePath;
    private String fileExtension;
    private String mimeType;
    private long fileSize;
}
