package com.aiworkforce.core.storage;

import com.aiworkforce.core.dto.FileUploadResponse;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "organization-logo",
            "avatar",
            "document",
            "project-image",
            "team-image"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Value("${app.storage.local-root:uploads}")
    private String localRoot;

    @Value("${app.storage.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    public FileUploadResponse store(MultipartFile file, String category) {
        String normalizedCategory = normalizeCategory(category);
        validate(file);

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;
        Path categoryDir = root().resolve(normalizedCategory).normalize();
        Path target = categoryDir.resolve(storedFilename).normalize();

        if (!target.startsWith(categoryDir)) {
            throw new BusinessException("Invalid file path");
        }

        try {
            Files.createDirectories(categoryDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("Cannot store uploaded file");
        }

        String internalPath = normalizedCategory + "/" + storedFilename;
        return FileUploadResponse.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .category(normalizedCategory)
                .contentType(file.getContentType())
                .size(file.getSize())
                .internalPath(internalPath)
                .downloadUrl("/api/v1/files/" + internalPath)
                .build();
    }

    public Resource load(String category, String filename) {
        String normalizedCategory = normalizeCategory(category);
        String safeFilename = sanitizeOriginalFilename(filename);
        Path categoryDir = root().resolve(normalizedCategory).normalize();
        Path target = categoryDir.resolve(safeFilename).normalize();
        if (!target.startsWith(categoryDir) || !Files.exists(target)) {
            throw new ResourceNotFoundException("File not found");
        }
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found");
        }
    }

    public String contentType(String category, String filename) {
        try {
            String detected = Files.probeContentType(root().resolve(normalizeCategory(category)).resolve(sanitizeOriginalFilename(filename)));
            return detected != null ? detected : "application/octet-stream";
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BusinessException("File size exceeds the allowed limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("Unsupported file type");
        }
    }

    private String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            throw new BusinessException("Unsupported file category");
        }
        return normalized;
    }

    private Path root() {
        return Paths.get(localRoot).toAbsolutePath().normalize();
    }

    private String sanitizeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        String sanitized = Paths.get(filename).getFileName().toString();
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isBlank() ? "file" : sanitized;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }
}
