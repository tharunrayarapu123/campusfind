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
public class ItemDetailDto {

    private Long id;
    private ItemType type;
    private String title;
    private String description;
    private ItemCategory category;
    private Long blockId;
    private String blockName;
    private Long roomId;
    private String roomName;
    private LocalDateTime eventTime;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private boolean handedToOffice;
    private String verificationQuestion;
    private ItemStatus status;
    private boolean hidden;
    private PublicUserDto owner; // Privacy rule: never exposes email or phone
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
