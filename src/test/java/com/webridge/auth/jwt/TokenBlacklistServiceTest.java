package com.webridge.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistServiceTest {

    private TokenBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new TokenBlacklistService(null);
    }

    @Test
    @DisplayName("블랙리스트에 등록된 토큰은 isBlacklisted()가 true여야 한다")
    void blacklistAndCheck() {
        String jti = "test-jti-12345";
        Date futureExp = new Date(System.currentTimeMillis() + 60_000);

        assertThat(blacklistService.isBlacklisted(jti)).isFalse();

        blacklistService.blacklist(jti, futureExp);
        assertThat(blacklistService.isBlacklisted(jti)).isTrue();
    }

    @Test
    @DisplayName("이미 만료된 시간으로 등록하면 블랙리스트에 추가되지 않는다")
    void expiredTokenNotBlacklisted() {
        String jti = "past-jti";
        Date pastExp = new Date(System.currentTimeMillis() - 1000);

        blacklistService.blacklist(jti, pastExp);
        assertThat(blacklistService.isBlacklisted(jti)).isFalse();
    }
}
