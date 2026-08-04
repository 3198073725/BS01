package com.vidsprout.common;

import com.vidsprout.modules.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 请求限流拦截器，对齐 Django DEFAULT_THROTTLE_RATES。
 * 匿名用户按 IP 限流，登录用户按 ID 限流。
 * 特定路径可配置独立阈值。
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiter;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.anonymous:100}")
    private int anonRate;

    @Value("${app.rate-limit.authenticated:1000}")
    private int userRate;

    private static final Duration WINDOW = Duration.ofHours(1);

    private static final Map<String, Integer> PATH_LIMITS = new LinkedHashMap<>();
    static {
        PATH_LIMITS.put("/api/users/register/", 5);
        PATH_LIMITS.put("/api/token/", 60);
        PATH_LIMITS.put("/api/users/login/send-code/", 30);
        PATH_LIMITS.put("/api/users/login/with-code/", 30);
        PATH_LIMITS.put("/api/videos/upload/", 20);
        PATH_LIMITS.put("/api/videos/upload/init/", 20);
        PATH_LIMITS.put("/api/videos/upload/chunk/", 100);
        PATH_LIMITS.put("/api/users/contact/submit/", 5);
        PATH_LIMITS.put("/api/users/login/qr/create/", 30);
        PATH_LIMITS.put("/api/users/login/qr/status/", 60);
        PATH_LIMITS.put("/api/users/avatar/upload/", 10);
    }

    public RateLimitInterceptor(RateLimiterService rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/schema") || path.startsWith("/static")) {
            return true;
        }

        String identifier;
        int limit;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            identifier = user.getId().toString();
            limit = userRate;
        } else {
            identifier = resolveIp(request);
            limit = anonRate;
        }

        for (Map.Entry<String, Integer> entry : PATH_LIMITS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                limit = entry.getValue();
                break;
            }
        }

        if (!rateLimiter.tryAcquire("http", identifier, limit, WINDOW)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"ok\":false,\"message\":\"请求过于频繁，请稍后再试\"}");
            } catch (Exception ignored) {
            }
            return false;
        }
        return true;
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim().toLowerCase();
        }
        String addr = request.getRemoteAddr();
        return addr != null ? addr.toLowerCase() : "unknown";
    }
}
