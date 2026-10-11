package com.campusfind.dto.response;

import com.campusfind.entity.ItemCategory;
import com.campusfind.entity.ItemStatus;
import com.campusfind.entity.ItemType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSummaryDto {

    private Long id;
    private ItemType type;
    private String title;
    private String description;
    private ItemCategory category;
    private String blockName;
    private String roomName;
    private LocalDateTime eventTime;
    private String imageUrl;
    private ItemStatus status;
    private boolean handedToOffice;
    private PublicUserDto owner; // Privacy rule: never exposes email or phone
    private LocalDateTime createdAt;
}
