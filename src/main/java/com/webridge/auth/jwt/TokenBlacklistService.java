package com.webridge.auth.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TokenBlacklistService {

    private final Optional<StringRedisTemplate> redisTemplate;
    private final Map<String, Long> inMemoryBlacklist = new ConcurrentHashMap<>();

    public TokenBlacklistService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = Optional.ofNullable(redisTemplate);
    }

    public void blacklist(String jti, Date expiration) {
        if (jti == null || expiration == null) {
            return;
        }

        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }

        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().opsForValue().set(
                    "jwt:blacklist:" + jti,
                    "1",
                    Duration.ofMillis(ttlMillis)
                );
                log.debug("Blacklisted token {} in Redis with TTL {} ms", jti, ttlMillis);
                return;
            } catch (Exception e) {
                log.warn("Redis blacklist failed, falling back to in-memory blacklist: {}", e.getMessage());
            }
        }

        inMemoryBlacklist.put(jti, expiration.getTime());
        log.debug("Blacklisted token {} in-memory", jti);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }

        if (redisTemplate.isPresent()) {
            try {
                Boolean hasKey = redisTemplate.get().hasKey("jwt:blacklist:" + jti);
                return Boolean.TRUE.equals(hasKey);
            } catch (Exception e) {
                log.warn("Redis check failed, checking in-memory blacklist: {}", e.getMessage());
            }
        }

        Long exp = inMemoryBlacklist.get(jti);
        if (exp != null) {
            if (System.currentTimeMillis() > exp) {
                inMemoryBlacklist.remove(jti);
                return false;
            }
            return true;
        }

        return false;
    }
}
