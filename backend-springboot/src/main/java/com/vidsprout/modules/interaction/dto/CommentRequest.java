package com.vidsprout.modules.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;

@Data
public class CommentRequest {
    @NotBlank
    private String content;
    private UUID videoId;
    private UUID parentId;
}
