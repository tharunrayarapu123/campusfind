package com.campusfind.controller;

import com.campusfind.dto.response.ApiResponse;
import com.campusfind.dto.response.HealthCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final Environment environment;
    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<ApiResponse<HealthCheckResponse>> checkHealth() {
        String dbStatus = "UNKNOWN";
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                dbStatus = "CONNECTED (" + connection.getMetaData().getDatabaseProductName() + " "
                        + connection.getMetaData().getDatabaseProductVersion() + ")";
            }
        } catch (Exception e) {
            dbStatus = "DISCONNECTED: " + e.getMessage();
        }

        String[] activeProfiles = environment.getActiveProfiles();
        String activeProfile = activeProfiles.length > 0 ? Arrays.toString(activeProfiles) : "default";

        HealthCheckResponse response = HealthCheckResponse.builder()
                .status("UP")
                .service("campusfind-backend")
                .activeProfile(activeProfile)
                .databaseStatus(dbStatus)
                .serverTime(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success("CampusFind service is operational", response));
    }
}
