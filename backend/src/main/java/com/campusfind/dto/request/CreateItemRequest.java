package com.campusfind.dto.request;

import com.campusfind.entity.ItemCategory;
import com.campusfind.entity.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateItemRequest {

    @NotNull(message = "Item type is required (LOST or FOUND)")
    private ItemType type;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private ItemCategory category;

    private Long blockId;

    private Long roomId;

    @NotNull(message = "Event time is required")
    @PastOrPresent(message = "Event time cannot be in the future")
    private LocalDateTime eventTime;

    private String imageUrl;

    private Double latitude;

    private Double longitude;

    @Builder.Default
    private boolean handedToOffice = false;

    private String verificationQuestion;
}
