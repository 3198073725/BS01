package com.vidsprout.modules.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    private UUID id;
    private String title;
    private String status;
    private String transcodeError;
    private String description;
    private Integer duration;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private Boolean allowComments;
    private Boolean allowDownload;
    private String visibility;
    private String uploadStatus;
    private UUID ownerId;
    private Boolean canEdit;
    private String videoUrl;
    private String lowMp4Url;
    private String thumbnailUrl;
    private String thumbnailVttUrl;
    private String hlsMasterUrl;
    private Long viewCount;
    private Long commentCount;
    private Long likeCount;
    private Long favoriteCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private Map<String, Object> author;
    private Boolean liked;
    private Boolean favorited;
    private Boolean watchLater;
    private Map<String, Object> category;
    private List<Map<String, Object>> tags;
}
