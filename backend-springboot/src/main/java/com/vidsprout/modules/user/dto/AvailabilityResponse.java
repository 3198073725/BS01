package com.vidsprout.modules.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailabilityResponse {
    private Boolean available;
    private String reason;
}
