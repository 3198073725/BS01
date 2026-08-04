package com.vidsprout.modules.video.controller;

import com.vidsprout.common.ApiResponse;
import com.vidsprout.modules.video.dto.*;
import com.vidsprout.modules.video.service.VideoService;
import com.vidsprout.modules.video.service.ThumbnailService;
import com.vidsprout.modules.video.service.HlsTranscodeService;
import com.vidsprout.modules.video.service.ContentModerationService;
import com.vidsprout.modules.recommendation.service.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    private final ThumbnailService thumbnailService;
    private final HlsTranscodeService hlsTranscodeService;
    private final ContentModerationService moderationService;
    private final RecommendationService recommendationService;

    public VideoController(VideoService videoService, ThumbnailService thumbnailService,
                           HlsTranscodeService hlsTranscodeService, ContentModerationService moderationService,
                           RecommendationService recommendationService) {
        this.videoService = videoService;
        this.thumbnailService = thumbnailService;
        this.hlsTranscodeService = hlsTranscodeService;
        this.moderationService = moderationService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/list/")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> listVideos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category_id,
            @RequestParam(required = false) String user_id,
            @RequestParam(required = false) String tag_ids,
            @RequestParam(required = false, defaultValue = "any") String tag_match,
            @RequestParam(required = false) String order,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false) Integer page_size,
            @RequestParam(required = false) Integer size) {
        String keyword = (q != null && !q.isEmpty()) ? q : search;
        UUID categoryId = parseUuid(category_id);
        UUID userId = parseUuid(user_id);
        List<UUID> tagIds = parseTagIds(tag_ids);
        int ps = (page_size != null && page_size > 0) ? page_size : (size != null && size > 0 ? size : 20);
        org.springframework.data.domain.Page<VideoResponse> result = videoService.listVideos(
                keyword, categoryId, tagIds, tag_match, userId, order, page, ps);
        return ResponseEntity.ok(ApiResponse.<List<VideoResponse>>builder()
                .ok(true)
                .results(result.getContent())
                .page(page)
                .pageSize(ps)
                .total((int) result.getTotalElements())
                .hasNext(result.hasNext())
                .build());
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<UUID> parseTagIds(String value) {
        if (value == null || value.isEmpty()) return null;
        String[] parts = value.split(",");
        List<UUID> ids = new ArrayList<>();
        for (String part : parts) {
            UUID id = parseUuid(part.trim());
            if (id != null) ids.add(id);
        }
        return ids;
    }

    @GetMapping("/{id}/")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideo(@PathVariable UUID id) {
        VideoResponse video = videoService.getVideoDetail(id);
        return ResponseEntity.ok(ApiResponse.success(video));
    }

    @PatchMapping("/{id}/")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideo(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates) {
        VideoResponse video = videoService.updateVideo(id, updates);
        return ResponseEntity.ok(ApiResponse.success(video));
    }

    @DeleteMapping("/{id}/")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(@PathVariable UUID id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    @PostMapping("/upload/")
    public ResponseEntity<ApiResponse<VideoResponse>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID categoryId) {
        ContentModerationService.ModerationResult modResult = moderationService.moderateText(title, description);
        if (!modResult.isAllowed()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("内容审核不通过：" + modResult.getReason()));
        }
        
        VideoResponse video = videoService.uploadVideo(file, title, description, categoryId);
        
        hlsTranscodeService.transcodeToHls(video.getId());
        hlsTranscodeService.generateVttAndThumbnails(video.getId());
        moderationService.moderateVideoAsync(video.getId());
        
        return ResponseEntity.ok(ApiResponse.success(video, "上传成功"));
    }

    @PostMapping("/upload/init/")
    public ResponseEntity<ApiResponse<ChunkUploadSession>> initChunkUpload(@RequestBody ChunkInitRequest request) {
        ChunkUploadSession session = videoService.initChunkUpload(request);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @PostMapping("/upload/chunk/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("upload_id") String uploadId,
            @RequestParam int index) {
        Map<String, Object> result = videoService.uploadChunk(uploadId, index, file);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/upload/status/")
    public ResponseEntity<ApiResponse<ChunkUploadStatus>> getUploadStatus(@RequestParam("id") String uploadId) {
        ChunkUploadStatus status = videoService.getUploadStatus(uploadId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping("/upload/complete/")
    public ResponseEntity<ApiResponse<VideoResponse>> completeChunkUpload(
            @RequestBody Map<String, String> body) {
        ContentModerationService.ModerationResult modResult = moderationService.moderateText(
                body.get("title"), body.get("description"));
        if (!modResult.isAllowed()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("内容审核不通过：" + modResult.getReason()));
        }
        
        VideoResponse video = videoService.completeChunkUpload(
                body.get("upload_id") != null ? body.get("upload_id") : body.get("uploadId"),
                body.get("title"), body.get("description"),
                body.containsKey("category_id") ? UUID.fromString(body.get("category_id")) : null);
        
        hlsTranscodeService.transcodeToHls(video.getId());
        hlsTranscodeService.generateVttAndThumbnails(video.getId());
        moderationService.moderateVideoAsync(video.getId());
        
        return ResponseEntity.ok(ApiResponse.success(video, "上传完成"));
    }

    @PostMapping("/{id}/retry-transcode/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retryTranscode(@PathVariable UUID id) {
        Map<String, Object> result = videoService.retryTranscode(id);
        hlsTranscodeService.transcodeToHls(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/thumbnail/pick/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pickThumbnail(
            @PathVariable UUID id,
            @RequestBody(required = false) ThumbnailPickRequest request) {
        double ts = request != null ? (request.getTs() != null ? request.getTs() : 
                                       request.getTime() != null ? request.getTime() : 1.0) : 1.0;
        Map<String, Object> result = thumbnailService.pickThumbnail(id, ts);
        return ResponseEntity.ok(ApiResponse.success(result, "封面设置成功"));
    }

    @PostMapping("/{id}/thumbnail/upload/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadThumbnail(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = thumbnailService.uploadThumbnail(id, file);
        return ResponseEntity.ok(ApiResponse.success(result, "封面上传成功"));
    }

    @PostMapping("/bulk-delete/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkDelete(@RequestBody Map<String, Object> body) {
        List<String> idsRaw = (List<String>) body.getOrDefault("video_ids", body.get("ids"));
        if (idsRaw == null || idsRaw.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("video_ids 必须为非空数组"));
        }
        List<UUID> ids = idsRaw.stream().map(UUID::fromString).toList();
        Map<String, Object> result = videoService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/bulk-update/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkUpdate(@RequestBody Map<String, Object> body) {
        List<String> idsRaw = (List<String>) body.getOrDefault("video_ids", body.get("ids"));
        if (idsRaw == null || idsRaw.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("video_ids 必须为非空数组"));
        }
        List<UUID> ids = idsRaw.stream().map(UUID::fromString).toList();
        Map<String, Object> updates = new HashMap<>();
        if (body.containsKey("allow_comments")) updates.put("allowComments", body.get("allow_comments"));
        if (body.containsKey("allow_download")) updates.put("allowDownload", body.get("allow_download"));
        if (body.containsKey("visibility")) updates.put("visibility", body.get("visibility"));
        Map<String, Object> result = videoService.bulkUpdate(ids, updates);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
