package com.campusfind.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardUserDto {

    private Long id;
    private String fullName;
    private String department;
    private Integer year;
    private String avatarUrl;
    private int karmaPoints;
    private List<String> badges;
    private long returnsCount;
}
