package com.webridge.auth.jwt;

import com.webridge.config.WebridgeProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtProvider(WebridgeProperties properties) {
        String secret = properties.jwt() != null && properties.jwt().secret() != null
            ? properties.jwt().secret()
            : "django-insecure-=u+s8tq&lcjxdfy_bcdz*1&0nr-ibh!hijfrl%=&65y+d_bvuv";

        // JJWT HS256 requires at least 256 bits (32 bytes). If provided secret is shorter, pad it safely.
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(secretBytes, 0, padded, 0, secretBytes.length);
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(secretBytes);
        }

        long accessHours = properties.jwt() != null && properties.jwt().accessTokenHours() > 0
            ? properties.jwt().accessTokenHours()
            : 6;
        long refreshDays = properties.jwt() != null && properties.jwt().refreshTokenDays() > 0
            ? properties.jwt().refreshTokenDays()
            : 30;

        this.accessTtl = Duration.ofHours(accessHours);
        this.refreshTtl = Duration.ofDays(refreshDays);
    }

    public String createAccess(Long userId) {
        return create(userId, "access", accessTtl, Map.of());
    }

    public String createRefresh(Long userId) {
        return create(userId, "refresh", refreshTtl, Map.of());
    }

    public String create(Long userId, String tokenType, Duration ttl, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString().replace("-", "");

        var builder = Jwts.builder()
            .claim("token_type", tokenType)
            .claim("user_id", userId)
            .id(jti)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttl)))
            .signWith(key, Jwts.SIG.HS256);

        if (extraClaims != null && !extraClaims.isEmpty()) {
            extraClaims.forEach(builder::claim);
        }

        return builder.compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(Claims claims) {
        Object val = claims.get("user_id");
        if (val instanceof Number n) {
            return n.longValue();
        } else if (val instanceof String s) {
            return Long.parseLong(s);
        }
        return null;
    }

    public String getTokenType(Claims claims) {
        return claims.get("token_type", String.class);
    }

    public String getJti(Claims claims) {
        String id = claims.getId();
        if (id != null) {
            return id;
        }
        return claims.get("jti", String.class);
    }
}
