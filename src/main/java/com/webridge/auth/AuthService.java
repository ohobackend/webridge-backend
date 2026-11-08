package com.webridge.auth;

import com.webridge.auth.dto.AuthResponse;
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
import com.webridge.auth.dto.UserDto;
import com.webridge.auth.dto.VerificationLinkResult;
import com.webridge.auth.jwt.JwtProvider;
import com.webridge.auth.jwt.TokenBlacklistService;
import com.webridge.auth.mail.MailService;
import com.webridge.auth.social.SocialUserInfo;
import com.webridge.auth.social.SocialUserInfoClient;
import com.webridge.common.exception.ApiException;
import com.webridge.config.WebridgeProperties;
import com.webridge.terms.entity.Terms;
import com.webridge.terms.repository.TermsRepository;
import com.webridge.user.entity.EmailVerification;
import com.webridge.user.entity.User;
import com.webridge.user.repository.EmailVerificationRepository;
import com.webridge.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TermsRepository termsRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final MailService mailService;
    private final SocialUserInfoClient socialUserInfoClient;
    private final WebridgeProperties props;

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!"email".equalsIgnoreCase(user.getProvider())) {
            String display = getProviderDisplay(user.getProvider());
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                Map.of(
                    "provider", user.getProvider(),
                    "message", "이 계정은 " + display + "로 로그인해주세요."
                )
            );
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "비활성화된 계정입니다.");
        }

        user.setLastLogin(OffsetDateTime.now());
        userRepository.save(user);

        String access = jwtProvider.createAccess(user.getId());
        String refresh = jwtProvider.createRefresh(user.getId());

        boolean needsTermsUpdate = checkNeedsTermsUpdate(user);
        return AuthResponse.of(UserDto.of(user, needsTermsUpdate), access, refresh, "로그인되었습니다.");
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (!req.password().equals(req.password2())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("password", List.of("비밀번호가 일치하지 않습니다.")));
        }

        if (!Boolean.TRUE.equals(req.serviceTermsAgreed())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("service_terms_agreed", List.of("서비스 이용약관에 동의해야 합니다.")));
        }
        if (!Boolean.TRUE.equals(req.ageVerificationAgreed())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("age_verification_agreed", List.of("만 14세 이상 확인에 동의해야 합니다.")));
        }
        if (!Boolean.TRUE.equals(req.privacyPolicyAgreed())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("privacy_policy_agreed", List.of("개인정보 수집 이용에 동의해야 합니다.")));
        }

        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("email", List.of("이미 가입된 이메일입니다.")));
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = User.builder()
            .email(req.email())
            .name(req.name())
            .password(passwordEncoder.encode(req.password()))
            .provider("email")
            .isActive(true)
            .isAdmin(false)
            .serviceTermsAgreed(true)
            .serviceTermsAgreedAt(now)
            .ageVerificationAgreed(true)
            .ageVerificationAgreedAt(now)
            .privacyPolicyAgreed(true)
            .privacyPolicyAgreedAt(now)
            .marketingAgreed(Boolean.TRUE.equals(req.marketingAgreed()))
            .marketingAgreedAt(Boolean.TRUE.equals(req.marketingAgreed()) ? now : null)
            .serviceTermsVersion("1.0")
            .privacyPolicyVersion("1.0")
            .marketingVersion("1.0")
            .build();

        userRepository.save(user);

        String access = jwtProvider.createAccess(user.getId());
        String refresh = jwtProvider.createRefresh(user.getId());

        return AuthResponse.of(UserDto.of(user, false), access, refresh, "회원가입이 완료되었습니다.");
    }

    public TokenRefreshResponse refreshToken(TokenRefreshRequest req) {
        try {
            Claims claims = jwtProvider.parse(req.refresh());
            String tokenType = jwtProvider.getTokenType(claims);
            String jti = jwtProvider.getJti(claims);

            if (!"refresh".equals(tokenType) || tokenBlacklistService.isBlacklisted(jti)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token이 만료되었거나 유효하지 않습니다.");
            }

            Long userId = jwtProvider.getUserId(claims);
            if (userId == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token이 만료되었거나 유효하지 않습니다.");
            }

            User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없거나 비활성화되었습니다."));

            // Blacklist the used refresh token (Rotation)
            Date exp = claims.getExpiration();
            tokenBlacklistService.blacklist(jti, exp);

            // Issue new access and refresh tokens
            String newAccess = jwtProvider.createAccess(user.getId());
            String newRefresh = jwtProvider.createRefresh(user.getId());

            return new TokenRefreshResponse(newAccess, newRefresh);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Token refresh failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token이 만료되었거나 유효하지 않습니다.");
        }
    }

    public void logout(LogoutRequest req) {
        try {
            Claims claims = jwtProvider.parse(req.refresh());
            String jti = jwtProvider.getJti(claims);
            Date exp = claims.getExpiration();
            tokenBlacklistService.blacklist(jti, exp);
        } catch (Exception e) {
            log.warn("Logout token blacklist failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_REQUEST, "토큰 블랙리스트 처리 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    public Map<String, Object> requestEmailVerification(EmailVerificationRequest req) {
        if (req == null || req.email() == null || req.email().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("email", List.of("이메일을 입력해주세요.")));
        }

        String email = req.email().trim();
        String verificationToken = UUID.randomUUID().toString();

        emailVerificationRepository.deleteByEmail(email);

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(30);
        EmailVerification verification = EmailVerification.builder()
            .email(email)
            .verificationToken(verificationToken)
            .expiresAt(expiresAt)
            .isVerified(false)
            .build();
        emailVerificationRepository.save(verification);

        String backendUrl = props.backendUrl() != null ? props.backendUrl() : "http://127.0.0.1:8080";
        String verificationLink = backendUrl + "/api/v1/auth/verify-email-link/?token=" + verificationToken;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분").withZone(ZoneId.of("Asia/Seoul"));
        String expiryTimeStr = formatter.format(expiresAt);

        mailService.sendHtml(email, "[WEBridge] 회원가입 인증 메일", "emails/verification_link", Map.of(
            "verification_url", verificationLink,
            "user_email", email,
            "user_name", email.split("@")[0],
            "site_name", "WEBridge",
            "company_name", "InAble",
            "expiry_time", expiryTimeStr
        ));

        return Map.of(
            "message", "인증 링크가 이메일로 발송되었습니다. (30분 유효)",
            "email", email
        );
    }

    @Transactional
    public VerificationLinkResult verifyEmailLink(String token) {
        if (token == null || token.isBlank()) {
            return new VerificationLinkResult(false, "유효하지 않은 인증 링크입니다.", null);
        }

        EmailVerification verification = emailVerificationRepository.findByVerificationToken(token).orElse(null);
        if (verification == null) {
            return new VerificationLinkResult(false, "유효하지 않은 인증 링크입니다.", null);
        }

        if (verification.isExpired()) {
            return new VerificationLinkResult(false, "인증 링크가 만료되었습니다. 다시 요청해주세요.", verification.getEmail());
        }

        if (verification.isVerified()) {
            return new VerificationLinkResult(true, "이미 인증된 이메일입니다.", verification.getEmail());
        }

        verification.setVerified(true);
        emailVerificationRepository.save(verification);

        return new VerificationLinkResult(true, "이메일 인증이 완료되었습니다!", verification.getEmail());
    }

    @Transactional(readOnly = true)
    public EmailVerificationCheckResponse checkEmailVerification(EmailVerificationRequest req) {
        if (req == null || req.email() == null || req.email().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("email", List.of("이메일을 입력해주세요.")));
        }

        String email = req.email().trim();
        EmailVerification verification = emailVerificationRepository.findByEmail(email).orElse(null);

        if (verification == null) {
            return new EmailVerificationCheckResponse(email, false, "인증 요청을 먼저 해주세요.");
        }

        if (verification.isVerified()) {
            return new EmailVerificationCheckResponse(email, true, "인증이 완료된 이메일입니다.");
        } else {
            return new EmailVerificationCheckResponse(email, false, "인증을 완료해주세요.");
        }
    }

    @Transactional
    public Map<String, String> requestPasswordReset(PasswordResetRequest req) {
        if (req == null || req.email() == null || req.email().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("email", List.of("이메일을 입력해주세요.")));
        }

        String email = req.email().trim();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            String resetToken = jwtProvider.create(
                user.getId(),
                "password_reset",
                Duration.ofMinutes(30),
                Map.of("email", user.getEmail(), "purpose", "password_reset")
            );
            String uid = Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(user.getId()).getBytes(StandardCharsets.UTF_8));
            String frontendUrl = props.frontendUrl() != null ? props.frontendUrl() : "http://localhost:3000";
            String resetLink = frontendUrl + "/reset-password?uid=" + uid + "&token=" + resetToken;

            mailService.sendHtml(user.getEmail(), "[WEBridge] 비밀번호 재설정 요청", "emails/password_reset", Map.of(
                "reset_url", resetLink,
                "user_email", user.getEmail(),
                "user_name", user.getName() != null && !user.getName().isBlank() ? user.getName() : user.getEmail().split("@")[0],
                "site_name", "WEBridge",
                "company_name", "InAble"
            ));
        }

        return Map.of("message", "비밀번호 재설정 링크가 이메일로 전송되었습니다. (30분 내 사용)");
    }

    @Transactional
    public Map<String, String> confirmPasswordReset(PasswordResetConfirmRequest req) {
        if (req == null || req.uid() == null || req.token() == null || req.newPassword() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "유효하지 않은 요청입니다."));
        }

        Long userId;
        try {
            String decodedUid = new String(Base64.getUrlDecoder().decode(req.uid()), StandardCharsets.UTF_8);
            userId = Long.parseLong(decodedUid);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "유효하지 않은 요청입니다."));
        }

        Claims claims;
        try {
            claims = jwtProvider.parse(req.token());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("expired") || msg.contains("exp")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "토큰이 만료되었습니다. 비밀번호 재설정을 다시 요청해주세요."));
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "유효하지 않은 토큰입니다."));
            }
        }

        String jti = jwtProvider.getJti(claims);
        if (tokenBlacklistService.isBlacklisted(jti)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "유효하지 않은 토큰입니다."));
        }

        if (!"password_reset".equals(jwtProvider.getTokenType(claims)) || !"password_reset".equals(claims.get("purpose", String.class))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "유효하지 않은 토큰입니다."));
        }

        Long tokenUserId = jwtProvider.getUserId(claims);
        if (tokenUserId == null || !tokenUserId.equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "유효하지 않은 요청입니다."));
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "유효하지 않은 요청입니다.")));

        if (!user.getEmail().equals(claims.get("email", String.class))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "유효하지 않은 토큰입니다."));
        }

        if (req.newPassword().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("new_password", List.of("비밀번호는 최소 8자 이상이어야 합니다.")));
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        tokenBlacklistService.blacklist(jti, claims.getExpiration());

        return Map.of("message", "비밀번호가 성공적으로 변경되었습니다.");
    }

    @Transactional
    public Map<String, String> changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));

        if (!"email".equalsIgnoreCase(user.getProvider())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다."));
        }

        if (req == null || req.newPassword() == null || req.newPassword().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("new_password", List.of("새 비밀번호를 입력해주세요.")));
        }

        if (req.newPassword().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("new_password", List.of("비밀번호는 최소 8자 이상이어야 합니다.")));
        }

        if (req.newPasswordConfirm() == null || !req.newPassword().equals(req.newPasswordConfirm())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("new_password_confirm", List.of("새 비밀번호가 일치하지 않습니다.")));
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        return Map.of("message", "비밀번호가 성공적으로 변경되었습니다.");
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> socialLogin(SocialLoginRequest req) {
        if (req == null || req.provider() == null || req.accessToken() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "제공자와 액세스 토큰은 필수 항목입니다.");
        }

        String provider = req.provider().toLowerCase();
        SocialUserInfo socialInfo = socialUserInfoClient.fetch(provider, req.accessToken());

        User user = userRepository.findByProviderAndProviderId(provider, socialInfo.id()).orElse(null);
        boolean isNew = false;

        if (user == null && socialInfo.email() != null && !socialInfo.email().isBlank()) {
            User existingEmailUser = userRepository.findByEmail(socialInfo.email()).orElse(null);
            if (existingEmailUser != null) {
                if (!provider.equalsIgnoreCase(existingEmailUser.getProvider())) {
                    String display = getProviderDisplay(existingEmailUser.getProvider());
                    throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        Map.of(
                            "status", "account_exists",
                            "message", String.format("이미 %s(으)로 가입된 이메일입니다.", display),
                            "provider", existingEmailUser.getProvider()
                        )
                    );
                }
                user = existingEmailUser;
            }
        }

        if (user == null) {
            isNew = true;
            String name = socialInfo.name() != null && !socialInfo.name().isBlank()
                ? socialInfo.name()
                : (socialInfo.email() != null && !socialInfo.email().isBlank() ? socialInfo.email().split("@")[0] : "소셜사용자");

            user = User.builder()
                .email(socialInfo.email())
                .name(name)
                .password(passwordEncoder.encode("!unusable_" + UUID.randomUUID()))
                .provider(provider)
                .providerId(socialInfo.id())
                .isActive(true)
                .isAdmin(false)
                .isSocialSignupComplete(false)
                .serviceTermsAgreed(false)
                .ageVerificationAgreed(false)
                .privacyPolicyAgreed(false)
                .marketingAgreed(false)
                .serviceTermsVersion("1.0")
                .privacyPolicyVersion("1.0")
                .marketingVersion("1.0")
                .build();

            user = userRepository.save(user);
        }

        if (user.needsSocialTermsAgreement()) {
            String access = jwtProvider.createAccess(user.getId());
            String refresh = jwtProvider.createRefresh(user.getId());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "terms_agreement_required");
            body.put("message", "서비스 이용을 위해 약관 동의가 필요합니다.");
            body.put("user_info", Map.of(
                "email", user.getEmail() != null ? user.getEmail() : "",
                "name", user.getName(),
                "provider", user.getProvider(),
                "profile_image", socialInfo.profileImage() != null ? socialInfo.profileImage() : ""
            ));
            body.put("access", access);
            body.put("refresh", refresh);
            body.put("is_new", isNew);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
        }

        String access = jwtProvider.createAccess(user.getId());
        String refresh = jwtProvider.createRefresh(user.getId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", UserDto.of(user, checkNeedsTermsUpdate(user)));
        body.put("access", access);
        body.put("refresh", refresh);
        body.put("is_new", isNew);

        return ResponseEntity.ok(body);
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> socialTermsAgreement(Long userId, SocialTermsAgreementRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));

        if ("email".equalsIgnoreCase(user.getProvider())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "이 기능은 소셜 로그인 사용자만 사용할 수 있습니다."));
        }

        if (!user.needsSocialTermsAgreement()) {
            return ResponseEntity.ok(Map.of(
                "message", "이미 약관 동의가 완료된 사용자입니다.",
                "user", UserDto.of(user, checkNeedsTermsUpdate(user))
            ));
        }

        if (req == null || !Boolean.TRUE.equals(req.serviceTermsAgreed())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("service_terms_agreed", List.of("서비스 이용약관에 동의해야 합니다.")));
        }
        if (!Boolean.TRUE.equals(req.ageVerificationAgreed())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("age_verification_agreed", List.of("만 14세 이상 확인에 동의해야 합니다.")));
        }
        if (!Boolean.TRUE.equals(req.privacyPolicyAgreed())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("privacy_policy_agreed", List.of("개인정보 수집 이용에 동의해야 합니다.")));
        }

        OffsetDateTime now = OffsetDateTime.now();
        user.setServiceTermsAgreed(true);
        user.setServiceTermsAgreedAt(now);
        user.setAgeVerificationAgreed(true);
        user.setAgeVerificationAgreedAt(now);
        user.setPrivacyPolicyAgreed(true);
        user.setPrivacyPolicyAgreedAt(now);
        user.setMarketingAgreed(Boolean.TRUE.equals(req.marketingAgreed()));
        user.setMarketingAgreedAt(Boolean.TRUE.equals(req.marketingAgreed()) ? now : null);
        user.setSocialSignupComplete(true);

        termsRepository.findByTypeAndIsActiveTrue("service").ifPresent(t -> user.setServiceTermsVersion(t.getVersion()));
        termsRepository.findByTypeAndIsActiveTrue("privacy").ifPresent(t -> user.setPrivacyPolicyVersion(t.getVersion()));
        if (user.isMarketingAgreed()) {
            termsRepository.findByTypeAndIsActiveTrue("marketing").ifPresent(t -> user.setMarketingVersion(t.getVersion()));
        }

        userRepository.save(user);

        String access = jwtProvider.createAccess(user.getId());
        String refresh = jwtProvider.createRefresh(user.getId());

        return ResponseEntity.ok(Map.of(
            "message", "약관 동의가 완료되었습니다.",
            "user", UserDto.of(user, false),
            "access", access,
            "refresh", refresh
        ));
    }

    private boolean checkNeedsTermsUpdate(User user) {
        if (user.isServiceTermsAgreed()) {
            Terms terms = termsRepository.findByTypeAndIsActiveTrue("service").orElse(null);
            if (terms != null && !terms.getVersion().equals(user.getServiceTermsVersion()) && terms.isRequiresReagreement()) {
                return true;
            }
        }
        if (user.isPrivacyPolicyAgreed()) {
            Terms terms = termsRepository.findByTypeAndIsActiveTrue("privacy").orElse(null);
            if (terms != null && !terms.getVersion().equals(user.getPrivacyPolicyVersion()) && terms.isRequiresReagreement()) {
                return true;
            }
        }
        if (user.isMarketingAgreed()) {
            Terms terms = termsRepository.findByTypeAndIsActiveTrue("marketing").orElse(null);
            if (terms != null && !terms.getVersion().equals(user.getMarketingVersion()) && terms.isRequiresReagreement()) {
                return true;
            }
        }
        return false;
    }

    private String getProviderDisplay(String provider) {
        if (provider == null) return "이메일";
        return switch (provider.toLowerCase()) {
            case "google" -> "Google";
            case "kakao" -> "Kakao";
            case "github" -> "GitHub";
            default -> provider;
        };
    }
}
