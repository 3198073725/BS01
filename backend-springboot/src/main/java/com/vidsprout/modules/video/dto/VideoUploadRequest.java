package com.vidsprout.modules.video.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class VideoUploadRequest {
    private String title;
    private String description;
    private UUID categoryId;
    private String visibility;
    private Boolean allowComments;
}
