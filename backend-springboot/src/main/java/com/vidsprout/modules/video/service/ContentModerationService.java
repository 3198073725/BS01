package com.vidsprout.modules.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidsprout.modules.video.model.Video;
import com.vidsprout.modules.video.repository.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class ContentModerationService {

    private final VideoRepository videoRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.moderation.api-key:}")
    private String moderationApiKey;

    @Value("${app.moderation.enabled:false}")
    private boolean moderationEnabled;

    @Value("${app.moderation.endpoint:}")
    private String moderationEndpoint;

    @Value("${app.upload.dir:./media}")
    private String uploadDir;

    public ContentModerationService(VideoRepository videoRepository, ObjectMapper objectMapper) {
        this.videoRepository = videoRepository;
        this.objectMapper = objectMapper;
    }

    public ModerationResult moderateText(String title, String description) {
        if (!moderationEnabled) {
            return new ModerationResult(true, null, null);
        }

        try {
            String combined = title + " " + description;
            
            if (containsBlockedKeywords(combined)) {
                return new ModerationResult(
                        false,
                        "内容包含敏感词汇",
                        List.of("blocked_keywords")
                );
            }

            Map<String, Double> scores = analyzeContent(combined);
            
                if (scores != null && !scores.isEmpty()) {
                    double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
                    if (maxScore > 0.8) {
                        return new ModerationResult(
                                false,
                                "内容不符合社区规范",
                                new ArrayList<>(scores.keySet())
                        );
                    }
                }

            return new ModerationResult(true, null, null);

        } catch (Exception e) {
            log.error("内容审核失败", e);
            return new ModerationResult(true, null, null);
        }
    }

    @Async
    public void moderateVideoAsync(UUID videoId) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) return;

        try {
            ModerationResult result = moderateText(video.getTitle(), video.getDescription());
            
            if (!result.isAllowed()) {
                video.setStatus("banned");
                video.setTranscodeError(result.getReason());
                videoRepository.save(video);
                log.warn("视频 {} 被审核拦截：{}", videoId, result.getReason());
            }

        } catch (Exception e) {
            log.error("异步审核失败：{}", videoId, e);
        }
    }

    private boolean containsBlockedKeywords(String text) {
        Set<String> blockedKeywords = new HashSet<>(Arrays.asList(
                "暴力", "恐怖", "色情", "赌博", "毒品", "诈骗",
                "违禁", "非法", "违法", "违规"
        ));
        
        String lowerText = text.toLowerCase();
        for (String keyword : blockedKeywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Double> analyzeContent(String text) {
        if (moderationEndpoint == null || moderationEndpoint.isEmpty() || moderationApiKey == null || moderationApiKey.isEmpty()) {
            return null;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(moderationEndpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + moderationApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(requestBody)
                    ))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                Map<String, Double> scores = new HashMap<>();
                
                JsonNode categories = root.path("results").path(0).path("category_scores");
                if (categories.isObject()) {
                    categories.fields().forEachRemaining(entry -> {
                        scores.put(entry.getKey(), entry.getValue().asDouble());
                    });
                }

                return scores;
            }

            return null;

        } catch (Exception e) {
            log.error("调用审核 API 失败", e);
            return null;
        }
    }

    public static class ModerationResult {
        private final boolean allowed;
        private final String reason;
        private final List<String> flaggedCategories;

        public ModerationResult(boolean allowed, String reason, List<String> flaggedCategories) {
            this.allowed = allowed;
            this.reason = reason;
            this.flaggedCategories = flaggedCategories;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }

        public List<String> getFlaggedCategories() {
            return flaggedCategories;
        }
    }
}
