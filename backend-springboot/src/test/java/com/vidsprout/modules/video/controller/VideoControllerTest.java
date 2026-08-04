package com.vidsprout.modules.video.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidsprout.common.RateLimiterService;
import com.vidsprout.modules.recommendation.service.RecommendationService;
import com.vidsprout.modules.video.dto.VideoResponse;
import com.vidsprout.modules.video.service.ContentModerationService;
import com.vidsprout.modules.video.service.HlsTranscodeService;
import com.vidsprout.modules.video.service.ThumbnailService;
import com.vidsprout.modules.video.service.VideoService;
import com.vidsprout.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = VideoController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class VideoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private VideoService videoService;
    @MockBean private ThumbnailService thumbnailService;
    @MockBean private HlsTranscodeService hlsTranscodeService;
    @MockBean private ContentModerationService moderationService;
    @MockBean private RecommendationService recommendationService;
    @MockBean private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(rateLimiterService.tryAcquire(anyString(), anyString(), anyInt(), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("视频列表接口测试")
    class ListVideosTests {

        @Test
        @DisplayName("GET /api/videos/list/ 应返回200")
        void shouldReturnVideoList() throws Exception {
            when(videoService.listVideos(eq(null), eq(null), eq(null), eq("any"), eq(null), eq(null), eq(1), eq(20)))
                    .thenReturn(org.springframework.data.domain.Page.empty());

            mockMvc.perform(get("/api/videos/list/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true));
        }
    }

    @Nested
    @DisplayName("视频详情接口测试")
    class GetVideoTests {

        @Test
        @DisplayName("GET /api/videos/{id}/ 应返回视频详情")
        void shouldReturnVideoDetail() throws Exception {
            UUID id = UUID.randomUUID();
            VideoResponse response = VideoResponse.builder()
                    .id(id).title("测试视频").duration(120)
                    .viewCount(100L).likeCount(10L).commentCount(5L).build();

            when(videoService.getVideoDetail(id)).thenReturn(response);

            mockMvc.perform(get("/api/videos/" + id + "/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("测试视频"))
                    .andExpect(jsonPath("$.data.duration").value(120));
        }
    }
}
