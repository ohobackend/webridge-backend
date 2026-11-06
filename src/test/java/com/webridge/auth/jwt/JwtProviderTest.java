package com.webridge.auth.jwt;

import com.webridge.config.WebridgeProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;
    private final String secret = "django-insecure-=u+s8tq&lcjxdfy_bcdz*1&0nr-ibh!hijfrl%=&65y+d_bvuv";

    @BeforeEach
    void setUp() {
        WebridgeProperties properties = new WebridgeProperties(
            new WebridgeProperties.Jwt(secret, 6, 30),
            "http://localhost:3000",
            "http://localhost:3000",
            "http://localhost:8080",
            "",
            "./reports",
            new WebridgeProperties.Scan(1, 900),
            new WebridgeProperties.Social(null, null, null)
        );
        jwtProvider = new JwtProvider(properties);
    }

    @Test
    @DisplayName("Access Token 생성 및 클레임(user_id, token_type, jti) 파싱이 정상 동작해야 한다")
    void createAndParseAccessToken() {
        Long userId = 42L;
        String token = jwtProvider.createAccess(userId);

        assertThat(token).isNotEmpty();
        assertThat(jwtProvider.validate(token)).isTrue();

        Claims claims = jwtProvider.parse(token);
        assertThat(jwtProvider.getUserId(claims)).isEqualTo(userId);
        assertThat(jwtProvider.getTokenType(claims)).isEqualTo("access");
        assertThat(jwtProvider.getJti(claims)).isNotEmpty();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("Refresh Token 생성 시 token_type이 refresh로 설정되어야 한다")
    void createAndParseRefreshToken() {
        Long userId = 99L;
        String token = jwtProvider.createRefresh(userId);

        assertThat(jwtProvider.validate(token)).isTrue();
        Claims claims = jwtProvider.parse(token);
        assertThat(jwtProvider.getUserId(claims)).isEqualTo(userId);
        assertThat(jwtProvider.getTokenType(claims)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("만료된 토큰은 validate()에서 false를 반환해야 한다")
    void expiredTokenValidation() {
        String expiredToken = jwtProvider.create(1L, "access", Duration.ofSeconds(-10), Map.of());
        assertThat(jwtProvider.validate(expiredToken)).isFalse();
    }
}
