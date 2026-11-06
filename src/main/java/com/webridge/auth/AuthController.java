package com.webridge.auth;

import com.webridge.auth.dto.AuthResponse;
import com.webridge.auth.dto.LoginRequest;
import com.webridge.auth.dto.LogoutRequest;
import com.webridge.auth.dto.RegisterRequest;
import com.webridge.auth.dto.TokenRefreshRequest;
import com.webridge.auth.dto.TokenRefreshResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
