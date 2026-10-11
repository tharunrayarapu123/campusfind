package com.campusfind.controller;

import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.ImageUploadResponse;
import com.campusfind.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Uploads", description = "Image upload service (Cloudinary / Local Fallback)")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image (JPEG, PNG, WebP up to 5 MB)")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file
    ) {
        ImageUploadResponse response = uploadService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
    }
}
