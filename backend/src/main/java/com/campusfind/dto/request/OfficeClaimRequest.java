package com.campusfind.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeClaimRequest {

    @NotBlank(message = "Description proof is required")
    @Size(max = 500, message = "Description proof must not exceed 500 characters")
    private String descriptionProof;
}
