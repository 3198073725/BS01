package com.vidsprout.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtUtils {

    private static final Set<String> WEAK_SECRETS = Set.of(
            "change-me-in-production-use-at-least-256-bits",
            "changeme", "secret", "password", "12345678",
            "0123456789abcdef0123456789abcdef"
    );

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final RedisTemplate<String, String> redisTemplate;

    public JwtUtils(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
                    @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration,
                    RedisTemplate<String, String> redisTemplate) {
        if (secret == null || secret.trim().isEmpty() || secret.trim().length() < 32) {
            throw new IllegalStateException("app.jwt.secret 必须配置且长度不少于 32 字节（通过环境变量 JWT_SECRET 注入）");
        }
        String trimmed = secret.trim();
        if (WEAK_SECRETS.contains(trimmed.toLowerCase())) {
            throw new IllegalStateException("app.jwt.secret 使用了公开占位符/弱值，禁止启动。请通过环境变量 JWT_SECRET 配置强随机密钥");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(UUID userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiration))
                .signWith(key)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get("type", String.class);
            if (!"access".equals(type)) return false;

            String userId = claims.getSubject();
            String cacheKey = "logout_after:" + userId;
            String cutoffStr = redisTemplate.opsForValue().get(cacheKey);
            if (cutoffStr != null) {
                long cutoff = Long.parseLong(cutoffStr);
                Date issuedAt = claims.getIssuedAt();
                if (issuedAt != null && issuedAt.getTime() / 1000 < cutoff) {
                    return false;
                }
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!"refresh".equals(claims.get("type", String.class))) return false;

            String userId = claims.getSubject();
            String cacheKey = "logout_after:" + userId;
            String cutoffStr = redisTemplate.opsForValue().get(cacheKey);
            if (cutoffStr != null) {
                long cutoff = Long.parseLong(cutoffStr);
                Date issuedAt = claims.getIssuedAt();
                if (issuedAt != null && issuedAt.getTime() / 1000 < cutoff) {
                    return false;
                }
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
