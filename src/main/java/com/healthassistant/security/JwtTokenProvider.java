package com.healthassistant.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expiration;
    private final long refreshExpiration;
    private final StringRedisTemplate redisTemplate;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration}") long expiration,
            @Value("${app.security.jwt.refresh-expiration}") long refreshExpiration,
            StringRedisTemplate redisTemplate) {
        this.secretKey = buildSecretKey(secret);
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(String username, Long userId, int userType) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("userType", userType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        String token = Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(secretKey)
                .compact();
        // Cache refresh token in Redis
        String key = "ha:jwt:refresh:" + username;
        redisTemplate.opsForValue().set(key, token, Duration.ofMillis(refreshExpiration));
        return token;
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public int getUserTypeFromToken(String token) {
        Integer type = parseClaims(token).get("userType", Integer.class);
        return type != null ? type : 1;
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return !isTokenBlacklisted(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public void blacklistToken(String token) {
        long ttl = parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            String key = "ha:jwt:blacklist:" + token;
            redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(ttl));
        }
    }

    private boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("ha:jwt:blacklist:" + token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey buildSecretKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                keyBytes = digest.digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 not available", e);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
