package com.campusfind.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "Full name cannot be blank")
    private String fullName;

    private String department;

    @Min(value = 1, message = "Year must be at least 1")
    @Max(value = 5, message = "Year must not exceed 5")
    private Integer year;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;

    private String avatarUrl;
}
