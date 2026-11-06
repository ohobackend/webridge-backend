package com.webridge.auth;

import com.webridge.auth.dto.AuthResponse;
import com.webridge.auth.dto.LoginRequest;
import com.webridge.auth.dto.LogoutRequest;
import com.webridge.auth.dto.RegisterRequest;
import com.webridge.auth.dto.TokenRefreshRequest;
import com.webridge.auth.dto.TokenRefreshResponse;
import com.webridge.auth.dto.UserDto;
import com.webridge.auth.jwt.JwtProvider;
import com.webridge.auth.jwt.TokenBlacklistService;
import com.webridge.common.exception.ApiException;
import com.webridge.terms.entity.Terms;
import com.webridge.terms.repository.TermsRepository;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TermsRepository termsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;

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
