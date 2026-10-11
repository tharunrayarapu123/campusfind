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
public class HealthCheckResponse {
    private String status;
    private String service;
    private String activeProfile;
    private String databaseStatus;
    private LocalDateTime serverTime;
}
