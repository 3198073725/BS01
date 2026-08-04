package com.vidsprout.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 滑动窗口限流，对齐 Django throttle 体系：
 * 对匿名用户用 IP，登录用户用 userId，按小时窗口计数。
 */
@Slf4j
@Component
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String prefix, String identifier, int maxRequests, Duration window) {
        long windowSeconds = window.getSeconds();
        long slot = System.currentTimeMillis() / 1000 / windowSeconds;
        String key = "ratelimit:" + prefix + ":" + identifier + ":" + slot;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, windowSeconds * 2, TimeUnit.SECONDS);
        }
        return count != null && count <= maxRequests;
    }
}
