package com.webridge.auth;

import com.webridge.auth.dto.AuthResponse;
import com.webridge.auth.dto.AuthUser;
import com.webridge.auth.dto.ChangePasswordRequest;
import com.webridge.auth.dto.EmailVerificationCheckResponse;
import com.webridge.auth.dto.EmailVerificationRequest;
import com.webridge.auth.dto.LoginRequest;
import com.webridge.auth.dto.LogoutRequest;
import com.webridge.auth.dto.PasswordResetConfirmRequest;
import com.webridge.auth.dto.PasswordResetRequest;
import com.webridge.auth.dto.RegisterRequest;
import com.webridge.auth.dto.SocialLoginRequest;
import com.webridge.auth.dto.SocialTermsAgreementRequest;
import com.webridge.auth.dto.TokenRefreshRequest;
import com.webridge.auth.dto.TokenRefreshResponse;
import com.webridge.auth.dto.VerificationLinkResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping({"/login", "/login/"})
    @Operation(summary = "로그인 API", description = "이메일과 비밀번호로 로그인합니다.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping({"/register", "/register/"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "회원가입 API", description = "신규 사용자를 등록합니다.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping({"/token/refresh", "/token/refresh/"})
    @Operation(summary = "토큰 갱신 API", description = "Refresh Token으로 새로운 Access Token과 Refresh Token을 발급받습니다.")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest req) {
        return ResponseEntity.ok(authService.refreshToken(req));
    }

    @PostMapping({"/logout", "/logout/"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "로그아웃 API", description = "Refresh Token을 블랙리스트에 추가하여 무효화합니다.")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/request-email-verification", "/request-email-verification/"})
    @Operation(summary = "이메일 인증 링크 발송", description = "회원가입 전 이메일로 인증 링크를 발송합니다.")
    public ResponseEntity<Map<String, Object>> requestEmailVerification(@RequestBody EmailVerificationRequest req) {
        return ResponseEntity.ok(authService.requestEmailVerification(req));
    }

    @GetMapping({"/verify-email-link", "/verify-email-link/"})
    @Operation(summary = "이메일 인증 링크 클릭 처리", description = "이메일 인증 링크 클릭 시 HTML 결과를 렌더링합니다.")
    public ModelAndView verifyEmailLink(@RequestParam(required = false) String token) {
        VerificationLinkResult result = authService.verifyEmailLink(token);
        ModelAndView mav = new ModelAndView("auth/verification_result");
        mav.addObject("success", result.success());
        mav.addObject("message", result.message());
        mav.addObject("email", result.email());
        return mav;
    }

    @PostMapping({"/check-email-verification", "/check-email-verification/"})
    @Operation(summary = "이메일 인증 상태 확인", description = "이메일 인증 완료 여부를 확인합니다.")
    public ResponseEntity<EmailVerificationCheckResponse> checkEmailVerification(@RequestBody EmailVerificationRequest req) {
        return ResponseEntity.ok(authService.checkEmailVerification(req));
    }

    @PostMapping({"/password-reset", "/password-reset/"})
    @Operation(summary = "비밀번호 재설정 요청", description = "비밀번호 재설정 링크를 이메일로 발송합니다.")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody PasswordResetRequest req) {
        return ResponseEntity.ok(authService.requestPasswordReset(req));
    }

    @PostMapping({"/password-reset-confirm", "/password-reset-confirm/"})
    @Operation(summary = "비밀번호 재설정 확인", description = "발급된 재설정 토큰으로 새로운 비밀번호를 설정합니다.")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest req) {
        return ResponseEntity.ok(authService.confirmPasswordReset(req));
    }

    @PostMapping({"/change-password", "/change-password/"})
    @Operation(summary = "로그인 사용자 비밀번호 변경", description = "현재 로그인된 사용자의 비밀번호를 변경합니다.")
    public ResponseEntity<Map<String, String>> changePassword(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestBody ChangePasswordRequest req
    ) {
        return ResponseEntity.ok(authService.changePassword(authUser.getId(), req));
    }

    @PostMapping({"/social-login", "/social-login/"})
    @Operation(summary = "소셜 로그인", description = "Google, Kakao, GitHub의 access token으로 로그인 또는 회원가입을 처리합니다.")
    public ResponseEntity<Map<String, Object>> socialLogin(@RequestBody SocialLoginRequest req) {
        return authService.socialLogin(req);
    }

    @PostMapping({"/social-terms-agreement", "/social-terms-agreement/"})
    @Operation(summary = "소셜 사용자 약관 동의", description = "소셜 로그인 후 필수 약관에 동의하여 회원가입을 완료합니다.")
    public ResponseEntity<Map<String, Object>> socialTermsAgreement(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestBody SocialTermsAgreementRequest req
    ) {
        return authService.socialTermsAgreement(authUser.getId(), req);
    }
}
