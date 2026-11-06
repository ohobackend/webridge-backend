package com.webridge.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.auth.dto.LoginRequest;
import com.webridge.auth.dto.LogoutRequest;
import com.webridge.auth.dto.RegisterRequest;
import com.webridge.auth.dto.TokenRefreshRequest;
import com.webridge.auth.jwt.JwtProvider;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("schematest")
@TestPropertySource(locations = "classpath:application-schematest.yml")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 -> 201 Created 응답과 사용자 정보 및 토큰을 반환해야 한다")
    void registerSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest(
            "testuser@webridge.com",
            "P@ssword1234!",
            "P@ssword1234!",
            "테스트유저",
            true,
            true,
            true,
            false
        );

        String json = mockMvc.perform(post("/api/v1/auth/register/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email").value("testuser@webridge.com"))
            .andExpect(jsonPath("$.user.name").value("테스트유저"))
            .andExpect(jsonPath("$.access").exists())
            .andExpect(jsonPath("$.refresh").exists())
            .andExpect(jsonPath("$.tokens.access").exists())
            .andExpect(jsonPath("$.tokens.refresh").exists())
            .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        String access = root.get("access").asText();
        assertThat(jwtProvider.validate(access)).isTrue();
    }

    @Test
    @DisplayName("로그인 -> 200 OK 응답과 토큰을 반환해야 한다")
    void loginSuccess() throws Exception {
        // Create user
        User user = User.builder()
            .email("loginuser@webridge.com")
            .name("로그인유저")
            .password(passwordEncoder.encode("LoginPassword1!"))
            .provider("email")
            .isActive(true)
            .serviceTermsAgreed(true)
            .ageVerificationAgreed(true)
            .privacyPolicyAgreed(true)
            .build();
        userRepository.save(user);

        LoginRequest req = new LoginRequest("loginuser@webridge.com", "LoginPassword1!");

        mockMvc.perform(post("/api/v1/auth/login/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("loginuser@webridge.com"))
            .andExpect(jsonPath("$.access").isNotEmpty())
            .andExpect(jsonPath("$.refresh").isNotEmpty());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 400 Bad Request와 에러 메시지를 반환해야 한다")
    void loginWrongPassword() throws Exception {
        User user = User.builder()
            .email("loginuser@webridge.com")
            .name("로그인유저")
            .password(passwordEncoder.encode("CorrectPassword!"))
            .provider("email")
            .isActive(true)
            .build();
        userRepository.save(user);

        LoginRequest req = new LoginRequest("loginuser@webridge.com", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("Django에서 기존 생성된 pbkdf2_sha256 해시를 가진 사용자도 정상 로그인되어야 한다")
    void loginExistingDjangoUser() throws Exception {
        // Pre-create user with a direct Django hash encoded password
        String rawPassword = "ExistingDjangoUserPassword!";
        String djangoHash = passwordEncoder.encode(rawPassword);

        User user = User.builder()
            .email("django_migrated@webridge.com")
            .name("마이그레이션유저")
            .password(djangoHash)
            .provider("email")
            .isActive(true)
            .serviceTermsAgreed(true)
            .ageVerificationAgreed(true)
            .privacyPolicyAgreed(true)
            .build();
        userRepository.save(user);

        LoginRequest req = new LoginRequest("django_migrated@webridge.com", rawPassword);

        mockMvc.perform(post("/api/v1/auth/login/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("django_migrated@webridge.com"))
            .andExpect(jsonPath("$.access").exists());
    }

    @Test
    @DisplayName("인증이 필요한 엔드포인트에 토큰 없이 접근 시 401과 DRF 호환 메시지를 반환해야 한다")
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        LogoutRequest req = new LogoutRequest("some-refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("자격 인증데이터(authentication credentials)가 제공되지 않았습니다."));
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증 엔드포인트 접근 시 성공해야 한다")
    void protectedEndpointWithValidToken() throws Exception {
        User user = User.builder()
            .email("authed@webridge.com")
            .name("인증유저")
            .password(passwordEncoder.encode("Pass123!"))
            .provider("email")
            .isActive(true)
            .build();
        userRepository.save(user);

        String access = jwtProvider.createAccess(user.getId());
        String refresh = jwtProvider.createRefresh(user.getId());
        LogoutRequest req = new LogoutRequest(refresh);

        mockMvc.perform(post("/api/v1/auth/logout/")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("토큰 갱신(Token Refresh) -> 새 access/refresh 반환 및 이전 refresh 토큰 블랙리스트 처리")
    void tokenRefreshAndRotation() throws Exception {
        User user = User.builder()
            .email("refresh@webridge.com")
            .name("리프레시유저")
            .password(passwordEncoder.encode("Pass123!"))
            .provider("email")
            .isActive(true)
            .build();
        userRepository.save(user);

        String refresh = jwtProvider.createRefresh(user.getId());
        TokenRefreshRequest req = new TokenRefreshRequest(refresh);

        String response = mockMvc.perform(post("/api/v1/auth/token/refresh/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access").exists())
            .andExpect(jsonPath("$.refresh").exists())
            .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        String newAccess = root.get("access").asText();
        String newRefresh = root.get("refresh").asText();

        assertThat(newAccess).isNotEmpty();
        assertThat(newRefresh).isNotEmpty();
        assertThat(newRefresh).isNotEqualTo(refresh);

        // Try using the OLD refresh token again -> should be 401 because it was blacklisted!
        mockMvc.perform(post("/api/v1/auth/token/refresh/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }
}
