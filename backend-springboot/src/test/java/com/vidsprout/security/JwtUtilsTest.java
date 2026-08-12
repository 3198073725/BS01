package com.vidsprout.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class JwtUtilsTest {

    private RedisTemplate<String, String> redisTemplate() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> t = mock(RedisTemplate.class);
        return t;
    }

    private void build(String secret) {
        new JwtUtils(secret, 604800000L, 5184000000L, redisTemplate());
    }

    @Test
    @DisplayName("公开占位符密钥拒绝启动")
    void rejectsPublicPlaceholderSecret() {
        assertThrows(IllegalStateException.class, () -> build("change-me-in-production-use-at-least-256-bits"));
    }

    @Test
    @DisplayName("过短密钥拒绝启动")
    void rejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> build("short"));
    }

    @Test
    @DisplayName("空密钥拒绝启动")
    void rejectsBlankSecret() {
        assertThrows(IllegalStateException.class, () -> build("   "));
    }

    @Test
    @DisplayName("强随机密钥允许启动")
    void acceptsStrongSecret() {
        build("0123456789abcdef0123456789abcdef0123456789abcdef");
    }
}
