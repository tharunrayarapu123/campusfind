package com.campusfind.service;

import com.campusfind.dto.response.ImageUploadResponse;
import com.campusfind.exception.ApiException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class UploadService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Value("${app.cloudinary.url:}")
    private String cloudinaryUrl;

    private Cloudinary cloudinary;
    private final org.springframework.core.env.Environment environment;

    public UploadService(org.springframework.core.env.Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        boolean isProd = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (cloudinaryUrl != null && !cloudinaryUrl.trim().isEmpty() && cloudinaryUrl.startsWith("cloudinary://")) {
            try {
                this.cloudinary = new Cloudinary(cloudinaryUrl);
                log.info("Cloudinary image upload service initialized successfully.");
            } catch (Exception e) {
                if (isProd) {
                    throw new IllegalStateException("CRITICAL CONFIG ERROR: Failed to initialize Cloudinary in prod profile: " + e.getMessage(), e);
                }
                log.warn("Failed to initialize Cloudinary with provided URL. Will fallback to local storage: {}", e.getMessage());
            }
        } else {
            if (isProd) {
                throw new IllegalStateException("CRITICAL CONFIG ERROR: In production profile 'prod', CLOUDINARY_URL is missing or invalid! Local file storage is disabled in production.");
            }
            log.warn("*******************************************************************************");
            log.warn("WARNING: CLOUDINARY_URL is not configured. Falling back to local /uploads storage.");
            log.warn("Uploaded images will be stored locally and served via /uploads/**.");
            log.warn("*******************************************************************************");
        }
    }

    public ImageUploadResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("No image file provided", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException("File size exceeds maximum allowed limit of 5 MB", HttpStatus.BAD_REQUEST);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException("Failed to read image bytes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String format = detectRealImageFormat(bytes);
        if (format == null) {
            throw new ApiException("Invalid image format. Only authentic JPEG, PNG, and WebP images are permitted", HttpStatus.BAD_REQUEST);
        }

        boolean isProd = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");

        // 1. Upload to Cloudinary if configured
        if (this.cloudinary != null) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                        "resource_type", "image",
                        "folder", "campusfind"
                ));
                String secureUrl = (String) uploadResult.get("secure_url");
                return ImageUploadResponse.builder()
                        .url(secureUrl)
                        .format(format)
                        .size(file.getSize())
                        .build();
            } catch (Exception e) {
                log.error("Cloudinary upload failed: {}", e.getMessage());
                if (isProd) {
                    throw new ApiException("Image upload to Cloudinary failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        if (isProd) {
            throw new ApiException("Local storage is disabled in production profile. Cloudinary must be configured.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 2. Fallback (Development & Standalone only): Save to local /uploads folder
        try {
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String filename = UUID.randomUUID().toString() + "." + format;
            Path targetPath = uploadDir.resolve(filename);
            Files.write(targetPath, bytes);

            String localUrl = "/uploads/" + filename;
            return ImageUploadResponse.builder()
                    .url(localUrl)
                    .format(format)
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new ApiException("Failed to save image to local storage: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Inspect file magic header bytes to verify authentic image format (JPEG, PNG, WebP)
     */
    private String detectRealImageFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }

        // JPEG: 0xFF, 0xD8, 0xFF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }

        // PNG: 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "png";
        }

        // WEBP: "RIFF" (0..3) and "WEBP" (8..11)
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }

        return null;
    }
}
