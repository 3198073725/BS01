package com.vidsprout.modules.user.dto;

import lombok.Data;

@Data
public class LoginWithCodeRequest {
    private String email;
    private String code;
}
