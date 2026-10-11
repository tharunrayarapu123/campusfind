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
public class ClaimResponse {

    private Long id;
    private Long matchId;
    private PublicUserDto claimant;
    private String answer;
    private Boolean approved;
    private boolean officeClaim;
    private LocalDateTime createdAt;
}
