package com.vidsprout.modules.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ChunkInitRequest {
    @NotBlank
    private String filename;
    @Positive
    private Long filesize;
    @Positive
    private Integer chunkSize;
}
