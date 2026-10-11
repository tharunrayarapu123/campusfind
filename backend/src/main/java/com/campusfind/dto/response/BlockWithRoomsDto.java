package com.campusfind.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockWithRoomsDto {
    private Long id;
    private String name;
    private List<RoomDto> rooms;
}
