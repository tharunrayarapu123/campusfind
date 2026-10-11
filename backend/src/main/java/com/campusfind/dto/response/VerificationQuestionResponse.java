package com.campusfind.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationQuestionResponse {

    private Long matchId;
    private String verificationQuestion;
    private boolean handedToOffice;
    private String instructions;
}
