package com.vidsprout.modules.analytics;

import com.vidsprout.common.exception.BusinessException;
import com.vidsprout.modules.analytics.service.AnalyticsService;
import com.vidsprout.modules.video.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private VideoRepository videoRepository;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void playEventIncrementsViewCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        UUID vid = UUID.randomUUID();

        int updated = analyticsService.ingestEvents(
                Map.of("type", "video_play", "video_id", vid.toString(), "session_id", "s1"),
                null, "1.2.3.4", null);

        assertEquals(1, updated);
        verify(videoRepository).incrementViewCount(vid);
    }

    @Test
    void duplicateEventIsDeduplicated() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true).thenReturn(false);
        UUID vid = UUID.randomUUID();
        Map<String, Object> ev = Map.of("type", "video_view", "video_id", vid.toString(), "session_id", "s1");

        int first = analyticsService.ingestEvents(ev, null, null, null);
        int second = analyticsService.ingestEvents(ev, null, null, null);

        assertEquals(1, first);
        assertEquals(0, second);
        verify(videoRepository, times(1)).incrementViewCount(vid);
    }

    @Test
    void differentSessionsCountSeparately() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        UUID vid = UUID.randomUUID();

        analyticsService.ingestEvents(Map.of("type", "play", "video_id", vid.toString(), "session_id", "a"), null, null, null);
        analyticsService.ingestEvents(Map.of("type", "play", "video_id", vid.toString(), "session_id", "b"), null, null, null);

        verify(videoRepository, times(2)).incrementViewCount(vid);
    }

    @Test
    void nonPlayEventIgnored() {
        int updated = analyticsService.ingestEvents(Map.of("type", "page_view"), null, null, null);

        assertEquals(0, updated);
        verify(videoRepository, never()).incrementViewCount(any(UUID.class));
    }

    @Test
    void missingVideoIdIgnored() {
        int updated = analyticsService.ingestEvents(Map.of("type", "video_play"), null, null, null);

        assertEquals(0, updated);
        verify(videoRepository, never()).incrementViewCount(any(UUID.class));
    }

    @Test
    void sessionHeaderUsedWhenSessionMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        UUID vid = UUID.randomUUID();

        analyticsService.ingestEvents(Map.of("type", "video_play", "video_id", vid.toString()), null, "9.9.9.9", "hdr-session");

        verify(redisTemplate.opsForValue())
                .setIfAbsent(eq("view_once:" + vid + ":hdr-session"), eq("1"), any(Duration.class));
    }

    @Test
    void listPayloadProcessed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        UUID v1 = UUID.randomUUID();
        UUID v2 = UUID.randomUUID();

        int updated = analyticsService.ingestEvents(List.of(
                Map.of("type", "video_play", "video_id", v1.toString(), "session_id", "s"),
                Map.of("type", "video_play", "video_id", v2.toString(), "session_id", "s"),
                Map.of("type", "click", "video_id", v2.toString())
        ), null, null, null);

        assertEquals(2, updated);
        verify(videoRepository).incrementViewCount(v1);
        verify(videoRepository).incrementViewCount(v2);
    }

    @Test
    void invalidVideoIdDoesNotBreak() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        int updated = analyticsService.ingestEvents(
                Map.of("type", "video_play", "video_id", "not-a-uuid", "session_id", "s"),
                null, null, null);

        assertEquals(0, updated);
    }

    @Test
    void scalarPayloadRejected() {
        assertThrows(BusinessException.class, () ->
                analyticsService.ingestEvents("hello", null, null, null));
    }

    @Test
    void nullPayloadRejected() {
        assertThrows(BusinessException.class, () ->
                analyticsService.ingestEvents(null, null, null, null));
    }
}
