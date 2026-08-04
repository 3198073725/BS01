package com.vidsprout.modules.user.dto;

import lombok.Data;

@Data
public class PasswordResetConfirmRequest {
    private String uid;
    private String token;
    private String newPassword;
}
