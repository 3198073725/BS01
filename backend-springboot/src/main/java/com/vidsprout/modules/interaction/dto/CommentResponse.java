package com.vidsprout.modules.interaction.dto;

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
public class CommentResponse {
    private UUID id;
    private String content;
    private Map<String, Object> user;
    private UUID video;
    private UUID parent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer repliesCount;
    private Boolean isLiked;
    private Integer likeCount;
    private List<CommentResponse> replies;
}
