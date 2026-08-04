package com.vidsprout.modules.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadSession {
    private String uploadId;
    private String filename;
    private Long filesize;
    private Integer chunkSize;
    private String ext;
}
