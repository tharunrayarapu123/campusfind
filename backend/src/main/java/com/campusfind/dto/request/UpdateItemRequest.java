package com.campusfind.dto.request;

import com.campusfind.entity.ItemCategory;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItemRequest {

    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private ItemCategory category;

    private Long blockId;

    private Long roomId;

    private String imageUrl;

    private Double latitude;

    private Double longitude;

    private Boolean handedToOffice;

    private String verificationQuestion;
}
