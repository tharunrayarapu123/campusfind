package com.campusfind.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserDto {

    private Long id;
    private String fullName;
    private String department;
    private Integer year;
    private String avatarUrl;
    private int karmaPoints;
}
