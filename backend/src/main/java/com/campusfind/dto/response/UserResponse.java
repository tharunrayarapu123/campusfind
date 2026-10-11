package com.campusfind.dto.response;

import com.campusfind.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String department;
    private Integer year;
    private String avatarUrl;
    private int karmaPoints;
    private Role role;
    private boolean banned;
    private LocalDateTime createdAt;
}
