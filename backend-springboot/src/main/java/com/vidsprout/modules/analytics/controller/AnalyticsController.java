package com.vidsprout.modules.analytics.controller;

import com.vidsprout.common.ApiResponse;
import com.vidsprout.modules.analytics.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/events/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestEvent(@RequestBody(required = false) Object payload,
                                                                        HttpServletRequest request) {
        int updated = analyticsService.ingestEvents(
                payload,
                request.getHeader("X-Forwarded-For"),
                request.getRemoteAddr(),
                request.getHeader("X-Session-Id"));
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", updated)));
    }
}
