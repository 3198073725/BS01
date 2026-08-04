package com.vidsprout.modules.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadStatus {
    private String uploadId;
    private String filename;
    private Long filesize;
    private int totalChunks;
    private List<Integer> receivedChunks;
    private List<Integer> missingChunks;
    private boolean complete;
}
