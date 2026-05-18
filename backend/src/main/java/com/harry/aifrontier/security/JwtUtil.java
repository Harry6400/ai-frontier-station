package com.harry.aifrontier.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret:}")
    private String secret;

    @Value("${app.jwt.expiration:86400000}")
    private long expiration;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.secret = validateOrGenerateSecret(this.secret);
        this.signingKey = deriveSigningKey(this.secret);
    }

    private String validateOrGenerateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            String generated = generateRandomSecret();
            log.warn("============================================================");
            log.warn("WARNING: JWT_SECRET 未配置，已生成随机密钥");
            log.warn("此密钥仅本次运行有效，重启后所有已颁发的 Token 将失效");
            log.warn("生产环境请务必配置 JWT_SECRET 环境变量");
            log.warn("============================================================");
            return generated;
        }
        if (secret.length() < 32) {
            log.warn("============================================================");
            log.warn("WARNING: JWT_SECRET 长度不足 32 字符，安全性较低");
            log.warn("建议使用至少 32 字符的随机字符串");
            log.warn("============================================================");
        }
        return secret;
    }

    private String generateRandomSecret() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return java.util.Base64.getEncoder().encodeToString(key);
    }

    private SecretKey deriveSigningKey(String secret) {
        try {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(keyBytes);
            return Keys.hmacShaKeyFor(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive JWT signing key", e);
        }
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }
}
