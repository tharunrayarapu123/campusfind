package com.campusfind.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveContentDto {

    private boolean hasFace;
    private boolean hasIdNumber;
    private boolean hasOtherPersonalInfo;
}
