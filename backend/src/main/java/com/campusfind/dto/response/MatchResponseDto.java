package com.campusfind.dto.response;

import com.campusfind.entity.MatchStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponseDto {

    private Long id;
    private ItemSummaryDto lostItem;
    private ItemSummaryDto foundItem;
    private Integer score;
    private Map<String, Object> scoreBreakdown;
    private MatchStatus status;
    private PublicUserDto otherUser; // The other matched student (safe profile only)
    private LocalDateTime createdAt;
}
