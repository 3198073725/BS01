package com.vidsprout.modules.user.dto;

import lombok.Data;

@Data
public class UserSearchRequest {
    private String q;
    private Boolean verified;
    private String order;
    private Integer page;
    private Integer size;
}
