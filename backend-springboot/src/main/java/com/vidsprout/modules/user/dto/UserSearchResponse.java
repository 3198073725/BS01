package com.vidsprout.modules.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserSearchResponse {
    private List<UserProfileResponse> results;
    private Integer count;
    private Integer page;
    private Integer size;
}
