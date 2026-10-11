package com.campusfind.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {

    private Long id;
    private Long adminId;
    private String adminEmail;
    private String action;
    private String targetType;
    private Long targetId;
    private String details;
    private LocalDateTime createdAt;
}
