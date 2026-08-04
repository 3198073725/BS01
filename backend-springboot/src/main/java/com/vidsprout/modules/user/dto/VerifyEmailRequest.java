package com.vidsprout.modules.user.dto;

import lombok.Data;

@Data
public class VerifyEmailRequest {
    private String uid;
    private String token;
}
