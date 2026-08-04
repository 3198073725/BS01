package com.vidsprout.modules.analytics.service;

import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.modules.video.repository.VideoRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 埋点事件服务，完整复刻 Django apps/analytics/views.py 的 EventsIngestView：
 * 对 video_play|video_view|play 事件按 (video_id, session_id/ip) 去重（TTL 6h）后累计 view_count。
 */
@Service
public class AnalyticsService {

    private static final long VIEW_DEDUPE_TTL_SECONDS = 6 * 3600;

    private final RedisTemplate<String, String> redisTemplate;
    private final VideoRepository videoRepository;

    public AnalyticsService(RedisTemplate<String, String> redisTemplate, VideoRepository videoRepository) {
        this.redisTemplate = redisTemplate;
        this.videoRepository = videoRepository;
    }

    public int ingestEvents(Object payload, String xForwardedFor, String remoteAddr, String sessionHeader) {
        List<Map<String, Object>> events = normalize(payload);
        String ip = resolveIp(xForwardedFor, remoteAddr);
        int updated = 0;
        for (Map<String, Object> ev : events) {
            String etype = String.valueOf(ev.getOrDefault("type", "")).toLowerCase();
            if (!isPlayEvent(etype)) {
                continue;
            }
            Object vid = ev.get("video_id") != null ? ev.get("video_id")
                    : ev.get("video") != null ? ev.get("video")
                    : ev.get("target_id");
            if (vid == null) {
                continue;
            }
            Object sidObj = ev.get("session_id");
            String sid = sidObj != null ? String.valueOf(sidObj)
                    : (sessionHeader != null && !sessionHeader.isEmpty() ? sessionHeader : ip);
            String key = "view_once:" + vid + ":" + sid;
            Boolean created = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofSeconds(VIEW_DEDUPE_TTL_SECONDS));
            if (!Boolean.TRUE.equals(created)) {
                continue;
            }
            try {
                videoRepository.incrementViewCount(UUID.fromString(vid.toString()));
                updated += 1;
            } catch (Exception ignored) {
                // 对齐 Django：video_id 无效或更新失败时跳过，不中断
            }
        }
        return updated;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalize(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            return List.of((Map<String, Object>) map);
        }
        if (payload instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    result.add((Map<String, Object>) m);
                }
            }
            return result;
        }
        throw new BusinessException("请求体应为对象或对象数组");
    }

    private boolean isPlayEvent(String etype) {
        return "video_play".equals(etype) || "video_view".equals(etype) || "play".equals(etype);
    }

    private String resolveIp(String xForwardedFor, String remoteAddr) {
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim().toLowerCase();
        }
        if (remoteAddr != null && !remoteAddr.isEmpty()) {
            return remoteAddr.toLowerCase();
        }
        return "unknown";
    }
}
