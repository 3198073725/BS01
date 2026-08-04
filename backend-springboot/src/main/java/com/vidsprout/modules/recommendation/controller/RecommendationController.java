package com.vidsprout.modules.recommendation.controller;

import com.vidsprout.common.ApiResponse;
import com.vidsprout.modules.recommendation.service.RecommendationService;
import com.vidsprout.modules.user.service.AuthService;
import com.vidsprout.modules.video.dto.VideoResponse;
import com.vidsprout.modules.video.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final VideoService videoService;
    private final AuthService authService;

    public RecommendationController(RecommendationService recommendationService,
                                    VideoService videoService,
                                    AuthService authService) {
        this.recommendationService = recommendationService;
        this.videoService = videoService;
        this.authService = authService;
    }

    @GetMapping("/feed/")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getRecommendationFeed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        try {
            var currentUser = authService.getCurrentUserEntityOrNull();
            var videos = recommendationService.getPersonalizedRecommendations(currentUser, page, pageSize);
            var responses = videos.stream().map(v -> videoService.toVideoResponseWithUserInteractions(v, currentUser)).toList();
            return feedResponse(responses, page, pageSize);
        } catch (Exception e) {
            log.error("获取推荐 feed 失败，回退热门", e);
            var currentUser = authService.getCurrentUserEntityOrNull();
            var videos = recommendationService.getTrendingVideos(page, pageSize);
            var responses = videos.stream().map(v -> videoService.toVideoResponseWithUserInteractions(v, currentUser)).toList();
            return feedResponse(responses, page, pageSize);
        }
    }

    @GetMapping("/following/")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getFollowingFeed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        var currentUser = authService.getCurrentUserEntityOrNull();
        var videos = recommendationService.getFollowingFeed(currentUser, page, pageSize);
        var responses = videos.stream().map(v -> videoService.toVideoResponseWithUserInteractions(v, currentUser)).toList();
        return feedResponse(responses, page, pageSize);
    }

    @GetMapping("/featured/")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getFeaturedFeed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        var videos = recommendationService.getTrendingVideos(page, pageSize);
        var currentUser = authService.getCurrentUserEntityOrNull();
        var responses = videos.stream().map(v -> videoService.toVideoResponseWithUserInteractions(v, currentUser)).toList();
        return feedResponse(responses, page, pageSize);
    }

    @GetMapping("/{videoId}/related/")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getRelatedVideos(
            @PathVariable UUID videoId,
            @RequestParam(defaultValue = "10") int limit) {
        var videos = recommendationService.getRelatedVideos(videoId, limit);
        var responses = videos.stream().map(videoService::toVideoResponse).toList();
        return ResponseEntity.ok(ApiResponse.successWithResults(responses, responses.size()));
    }

    private ResponseEntity<ApiResponse<List<VideoResponse>>> feedResponse(List<VideoResponse> responses, int page, int pageSize) {
        int size = Math.max(1, pageSize);
        return ResponseEntity.ok(ApiResponse.<List<VideoResponse>>builder()
                .ok(true)
                .results(responses)
                .page(page)
                .pageSize(size)
                .total(responses.size())
                .hasNext(!responses.isEmpty())
                .build());
    }
}
