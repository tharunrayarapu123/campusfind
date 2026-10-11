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
public class ReportDto {

    private Long id;
    private Long itemId;
    private String itemTitle;
    private PublicUserDto reporter;
    private String reason;
    private LocalDateTime createdAt;
}
