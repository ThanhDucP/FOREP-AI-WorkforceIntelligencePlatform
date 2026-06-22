package com.aiworkforce.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String originalFilename;
    private String storedFilename;
    private String category;
    private String contentType;
    private long size;
    private String internalPath;
    private String downloadUrl;
}
