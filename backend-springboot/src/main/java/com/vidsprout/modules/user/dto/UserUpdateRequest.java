package com.vidsprout.modules.user.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {
    private String nickname;
    private String bio;
    private String gender;
    private LocalDate birthDate;
    private String location;
    private String website;
    private String privacyMode;
}
