package com.campusfind.dto.response;

import com.campusfind.entity.ItemCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageAnalysisResponse {

    private ItemCategory category;
    private String title;
    private String color;
    private String brand;
    private List<String> tags;
    private String shortDescription;
    private SensitiveContentDto sensitiveContent;
    private String warningMessage;
}
