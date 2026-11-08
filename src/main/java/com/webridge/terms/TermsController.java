package com.webridge.terms;

import com.webridge.auth.dto.AuthUser;
import com.webridge.terms.dto.TermsUpdateRequest;
import com.webridge.terms.dto.TermsUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
@Tag(name = "Terms", description = "약관 조회 및 동의 상태 관리 API")
public class TermsController {

    private final TermsService termsService;

    @GetMapping({"/current", "/current/"})
    @Operation(summary = "현재 약관 조회", description = "현재 유효한 최신 약관들을 조회합니다.")
    public ResponseEntity<Map<String, Object>> getCurrentTerms() {
        return ResponseEntity.ok(termsService.getCurrentTerms());
    }

    @GetMapping({"/status", "/status/"})
    @Operation(summary = "사용자 약관 상태 확인", description = "로그인한 사용자의 약관 동의 상태와 업데이트 필요 여부를 확인합니다.")
    public ResponseEntity<Map<String, Object>> checkTermsStatus(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(termsService.checkTermsStatus(authUser.getId()));
    }

    @PostMapping({"/update", "/update/"})
    @Operation(summary = "약관 재동의", description = "업데이트된 약관에 대해 재동의를 진행합니다.")
    public ResponseEntity<TermsUpdateResponse> updateTermsAgreement(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestBody TermsUpdateRequest request
    ) {
        return ResponseEntity.ok(termsService.updateTermsAgreement(authUser.getId(), request));
    }
}
