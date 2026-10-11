package com.campusfind.controller;

import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.ImageAnalysisResponse;
import com.campusfind.entity.User;
import com.campusfind.exception.ResourceNotFoundException;
import com.campusfind.repository.UserRepository;
import com.campusfind.service.ai.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI vision analysis, auto-tagging, and sensitive content detection")
public class AiController {

    private final AiAnalysisService aiAnalysisService;
    private final UserRepository userRepository;

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze item image using Gemini Vision AI (auto-tag, category suggest, sensitive content warning)")
    public ResponseEntity<ApiResponse<ImageAnalysisResponse>> analyzeImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()));

        ImageAnalysisResponse response = aiAnalysisService.analyzeItemImage(user.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Image analyzed successfully", response));
    }
}
