package com.vidsprout.modules.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsernameChangeResponse {
    private String username;
    private Integer coolDownDays;
}
