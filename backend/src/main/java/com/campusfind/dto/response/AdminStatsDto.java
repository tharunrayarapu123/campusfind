package com.campusfind.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDto {

    @Builder.Default
    private String status = "HEALTHY";
    private long totalUsers;
    private long totalLost;
    private long totalFound;
    private long returned;
    private double returnRatePercentage;
    private long openItems;
    private long matchesCount;
    private Map<String, Long> itemsPerBlock;
}
