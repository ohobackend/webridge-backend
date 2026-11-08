package com.webridge.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.auth.dto.ChangePasswordRequest;
import com.webridge.auth.dto.EmailVerificationRequest;
import com.webridge.auth.dto.PasswordResetConfirmRequest;
import com.webridge.auth.dto.PasswordResetRequest;
import com.webridge.auth.dto.SocialLoginRequest;
import com.webridge.auth.dto.SocialTermsAgreementRequest;
import com.webridge.auth.jwt.JwtProvider;
import com.webridge.auth.social.SocialUserInfo;
import com.webridge.auth.social.SocialUserInfoClient;
import com.webridge.user.entity.EmailVerification;
import com.webridge.user.entity.User;
import com.webridge.user.repository.EmailVerificationRepository;
import com.webridge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("schematest")
class Phase4AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @MockBean
    private SocialUserInfoClient socialUserInfoClient;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() {
        emailVerificationRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
            .email("phase4@example.com")
            .name("인증유저")
            .password(passwordEncoder.encode("OldPassword123!"))
            .provider("email")
            .isActive(true)
            .serviceTermsAgreed(true)
            .privacyPolicyAgreed(true)
            .marketingAgreed(false)
            .serviceTermsVersion("1.0")
            .privacyPolicyVersion("1.0")
            .marketingVersion("1.0")
            .build());

        token = jwtProvider.createAccess(testUser.getId());
    }

    // ==========================================
    // 1. Email Verification Flow
    // ==========================================

    @Test
    @DisplayName("이메일 인증 링크 요청 -> DB 토큰 생성 -> 링크 클릭 (HTML) -> 상태 확인 (JSON) 완료")
    void emailVerificationFlow() throws Exception {
        String testEmail = "verify@example.com";

        // 1. 이메일 인증 요청
        mockMvc.perform(post("/api/v1/auth/request-email-verification/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EmailVerificationRequest(testEmail))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(testEmail))
            .andExpect(jsonPath("$.message").value("인증 링크가 이메일로 발송되었습니다. (30분 유효)"));

        // DB에서 생성된 토큰 확인
        EmailVerification verification = emailVerificationRepository.findByEmail(testEmail).orElseThrow();
        assertThat(verification.isVerified()).isFalse();
        String verificationToken = verification.getVerificationToken();

        // 2. 인증 전 상태 확인 -> false
        mockMvc.perform(post("/api/v1/auth/check-email-verification/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EmailVerificationRequest(testEmail))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(false))
            .andExpect(jsonPath("$.message").value("인증을 완료해주세요."));

        // 3. 인증 링크 클릭 (GET) -> HTML 렌더링 및 인증 완료
        mockMvc.perform(get("/api/v1/auth/verify-email-link/")
                .param("token", verificationToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("이메일 인증이 성공적으로 완료되었습니다")));

        // 4. 인증 완료 후 상태 확인 -> true
        mockMvc.perform(post("/api/v1/auth/check-email-verification/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EmailVerificationRequest(testEmail))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true))
            .andExpect(jsonPath("$.message").value("인증이 완료된 이메일입니다."));

        // 5. 잘못된 토큰 클릭 -> 실패 안내 HTML
        mockMvc.perform(get("/api/v1/auth/verify-email-link/")
                .param("token", "invalid-uuid-token"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("유효하지 않은 인증 링크입니다.")));
    }

    // ==========================================
    // 2. Password Reset Flow
    // ==========================================

    @Test
    @DisplayName("비밀번호 재설정 요청 -> 토큰 검증 및 비밀번호 변경 -> 새 비밀번호로 로그인 성공")
    void passwordResetFlow() throws Exception {
        // 1. 비밀번호 재설정 요청
        mockMvc.perform(post("/api/v1/auth/password-reset/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetRequest(testUser.getEmail()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("비밀번호 재설정 링크가 이메일로 전송되었습니다. (30분 내 사용)"));

        // 토큰과 uid 생성
        String resetToken = jwtProvider.create(
            testUser.getId(),
            "password_reset",
            Duration.ofMinutes(30),
            Map.of("email", testUser.getEmail(), "purpose", "password_reset")
        );
        String uid = Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(testUser.getId()).getBytes(StandardCharsets.UTF_8));

        // 2. 비밀번호 재설정 확인 (새 비밀번호 8자 미만 -> 400 Bad Request)
        mockMvc.perform(post("/api/v1/auth/password-reset-confirm/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetConfirmRequest(uid, resetToken, "short"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.new_password[0]").value("비밀번호는 최소 8자 이상이어야 합니다."));

        // 3. 비밀번호 재설정 정상 확인
        mockMvc.perform(post("/api/v1/auth/password-reset-confirm/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetConfirmRequest(uid, resetToken, "BrandNewPassword123!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."));

        // 4. 재사용 방지 확인 (블랙리스트에 등록되어 거부)
        mockMvc.perform(post("/api/v1/auth/password-reset-confirm/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordResetConfirmRequest(uid, resetToken, "AnotherPassword123!"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("유효하지 않은 토큰입니다."));

        // 5. 새 비밀번호로 로그인 시도 -> 200 OK
        mockMvc.perform(post("/api/v1/auth/login/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", testUser.getEmail(), "password", "BrandNewPassword123!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access").exists());
    }

    // ==========================================
    // 3. Change Password Flow (로그인 사용자)
    // ==========================================

    @Test
    @DisplayName("로그인된 사용자의 비밀번호 변경 -> 불일치 시 400, 일치 시 성공")
    void changePasswordFlow() throws Exception {
        // 비밀번호 불일치 시 400
        mockMvc.perform(post("/api/v1/auth/change-password/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangePasswordRequest("NewPass1234!", "Different1234!"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.new_password_confirm[0]").value("새 비밀번호가 일치하지 않습니다."));

        // 정상 변경
        mockMvc.perform(post("/api/v1/auth/change-password/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangePasswordRequest("NewPass1234!", "NewPass1234!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."));

        // 새 비밀번호로 로그인 성공 확인
        mockMvc.perform(post("/api/v1/auth/login/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", testUser.getEmail(), "password", "NewPass1234!"))))
            .andExpect(status().isOk());
    }

    // ==========================================
    // 4. Social Login Flow (Google, Kakao, GitHub)
    // ==========================================

    @Test
    @DisplayName("신규 구글 소셜 로그인 -> 202 Accepted (약관 동의 필요) -> 약관 동의 제출 -> 로그인 완료")
    void socialLoginAndTermsAgreementFlow() throws Exception {
        // Mock Google response
        Mockito.when(socialUserInfoClient.fetch(eq("google"), eq("google_mock_token")))
            .thenReturn(new SocialUserInfo("google_12345", "googleuser@example.com", "구글유저", "https://img.com/p.png"));

        // 1. 신규 소셜 로그인 시도 -> 202 Accepted (약관 동의 필요)
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/social-login/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SocialLoginRequest("google", "google_mock_token"))))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("terms_agreement_required"))
            .andExpect(jsonPath("$.is_new").value(true))
            .andExpect(jsonPath("$.user_info.email").value("googleuser@example.com"))
            .andReturn();

        String respStr = loginResult.getResponse().getContentAsString();
        String tempAccessToken = objectMapper.readTree(respStr).get("access").asText();

        // 2. 약관 미동의 시 400
        mockMvc.perform(post("/api/v1/auth/social-terms-agreement/")
                .header("Authorization", "Bearer " + tempAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SocialTermsAgreementRequest(false, true, true, false))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.service_terms_agreed[0]").value("서비스 이용약관에 동의해야 합니다."));

        // 3. 약관 필수 동의 완료 -> 200 OK
        mockMvc.perform(post("/api/v1/auth/social-terms-agreement/")
                .header("Authorization", "Bearer " + tempAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SocialTermsAgreementRequest(true, true, true, false))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("약관 동의가 완료되었습니다."))
            .andExpect(jsonPath("$.user.email").value("googleuser@example.com"));

        // 4. 이미 가입 및 동의된 상태에서 다시 소셜 로그인 -> 200 OK
        mockMvc.perform(post("/api/v1/auth/social-login/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SocialLoginRequest("google", "google_mock_token"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("googleuser@example.com"))
            .andExpect(jsonPath("$.is_new").value(false));
    }

    @Test
    @DisplayName("기존 이메일 계정으로 가입된 이메일로 소셜 로그인 시도 시 400 account_exists 반환")
    void socialLoginWithExistingEmailDifferentProvider() throws Exception {
        Mockito.when(socialUserInfoClient.fetch(eq("kakao"), eq("kakao_mock_token")))
            .thenReturn(new SocialUserInfo("kakao_99999", testUser.getEmail(), "카카오유저", ""));

        mockMvc.perform(post("/api/v1/auth/social-login/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SocialLoginRequest("kakao", "kakao_mock_token"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("account_exists"))
            .andExpect(jsonPath("$.provider").value("email"));
    }
}
