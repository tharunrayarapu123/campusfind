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
public class ChatMessageDto {

    private Long id;
    private Long matchId;
    private PublicUserDto sender;
    private String body;
    private String imageUrl;
    private LocalDateTime createdAt;
}
