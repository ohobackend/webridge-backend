package com.webridge.admin;

import com.webridge.admin.dto.*;
import com.webridge.common.dto.PaginatedResponse;
import com.webridge.scan.dto.ScanDetailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API")
public class AdminController {

    private final AdminService adminService;

    // ==========================================
    // 0. 관리자 로그인 (Admin Login)
    // ==========================================

    @PostMapping({"/login", "/login/"})
    @Operation(summary = "관리자 로그인", description = "관리자 전용 계정(ID: inableit / PW: inableit2025)으로 로그인하여 JWT 토큰을 발급받습니다.")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest req) {
        return ResponseEntity.ok(adminService.login(req));
    }

    // ==========================================
    // 1. 대시보드 (Dashboard)
    // ==========================================

    @GetMapping({"/dashboard", "/dashboard/"})
    @Operation(summary = "관리자 대시보드 통계", description = "회원 수, 오늘 신규 가입, 검사 통계 및 7일간의 추이 데이터를 반환합니다.")
    public ResponseEntity<AdminDashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ==========================================
    // 2. 회원 관리 (User Management)
    // ==========================================

    @GetMapping({"/users", "/users/"})
    @Operation(summary = "회원 목록 조회", description = "검색어(이름/이메일), 관리자 여부, 활성 여부, 가입 제공자(provider)로 필터링하여 회원 목록을 페이징 조회합니다.")
    public ResponseEntity<PaginatedResponse<AdminUserSummaryDto>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isAdmin,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String provider,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getUsers(search, isAdmin, isActive, provider, page, size));
    }

    @GetMapping({"/users/{userId}", "/users/{userId}/"})
    @Operation(summary = "회원 상세 정보 조회", description = "회원의 기본 정보, 가입 시 설문 정보(유입경로/직군), 약관 동의 내역, 최근 검사 내역을 조회합니다.")
    public ResponseEntity<AdminUserDetailDto> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserDetail(userId));
    }

    @PatchMapping({"/users/{userId}/status", "/users/{userId}/status/"})
    @Operation(summary = "회원 활성/비활성 상태 변경", description = "회원을 정지/비활성화하거나 다시 활성화합니다.")
    public ResponseEntity<AdminUserSummaryDto> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest req
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, req.isActive()));
    }

    @PatchMapping({"/users/{userId}/role", "/users/{userId}/role/"})
    @Operation(summary = "회원 관리자 권한 변경", description = "일반 회원을 관리자로 지정하거나 관리자 권한을 해제합니다.")
    public ResponseEntity<AdminUserSummaryDto> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleUpdateRequest req
    ) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, req.isAdmin()));
    }

    // ==========================================
    // 3. 검사 내역 관리 (Scan Management)
    // ==========================================

    @GetMapping({"/scans", "/scans/"})
    @Operation(summary = "전체 웹 접근성 검사 내역 조회", description = "모든 사용자의 웹 접근성 검사 기록을 검색 및 상태 필터링하여 페이징 조회합니다.")
    public ResponseEntity<PaginatedResponse<AdminScanSummaryDto>> getScans(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getScans(search, status, page, size));
    }

    @GetMapping({"/scans/{scanId}", "/scans/{scanId}/"})
    @Operation(summary = "검사 상세 결과 조회", description = "특정 검사의 13개 항목 상세 진단 결과를 조회합니다.")
    public ResponseEntity<ScanDetailDto> getScanDetail(@PathVariable UUID scanId) {
        return ResponseEntity.ok(adminService.getScanDetail(scanId));
    }

    @DeleteMapping({"/scans/{scanId}", "/scans/{scanId}/"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "검사 기록 삭제", description = "문제가 있거나 불필요한 검사 기록을 삭제합니다.")
    public ResponseEntity<Void> deleteScan(@PathVariable UUID scanId) {
        adminService.deleteScan(scanId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 4. 약관 관리 (Terms Management)
    // ==========================================

    @GetMapping({"/terms", "/terms/"})
    @Operation(summary = "전체 약관 목록 조회", description = "서비스 이용약관, 개인정보 처리방침, 마케팅 동의 등 모든 약관 목록을 조회합니다.")
    public ResponseEntity<List<AdminTermsDto>> getAllTerms() {
        return ResponseEntity.ok(adminService.getAllTerms());
    }

    @GetMapping({"/terms/{termsId}", "/terms/{termsId}/"})
    @Operation(summary = "약관 상세 조회", description = "약관의 내용 및 세부 설정 정보를 조회합니다.")
    public ResponseEntity<AdminTermsDto> getTermsDetail(@PathVariable Long termsId) {
        return ResponseEntity.ok(adminService.getTermsDetail(termsId));
    }

    @PostMapping({"/terms", "/terms/"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "새 약관 등록", description = "새로운 약관 항목을 등록합니다.")
    public ResponseEntity<AdminTermsDto> createTerms(@Valid @RequestBody AdminTermsCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createTerms(req));
    }

    @PutMapping({"/terms/{termsId}", "/terms/{termsId}/"})
    @Operation(summary = "약관 수정", description = "약관 버전, 내용, 재동의 필요 여부 등을 수정합니다.")
    public ResponseEntity<AdminTermsDto> updateTerms(
            @PathVariable Long termsId,
            @Valid @RequestBody AdminTermsUpdateRequest req
    ) {
        return ResponseEntity.ok(adminService.updateTerms(termsId, req));
    }

    // ==========================================
    // 5. 가입 설문조사 통계 (유입경로 & 직군)
    // ==========================================

    @GetMapping({"/surveys/signup", "/surveys/signup/"})
    @Operation(summary = "가입 시 유입경로 및 직군 설문 통계", description = "가입 시 사용자가 선택한 유입 경로 및 직군의 통계(비율, 응답수, 기타 응답)를 확인합니다.")
    public ResponseEntity<AdminSignupSurveyStatsDto> getSignupSurveyStats() {
        return ResponseEntity.ok(adminService.getSignupSurveyStats());
    }

    // ==========================================
    // 6. AI 대체텍스트 피드백 관리 (Alt Text Feedback)
    // ==========================================

    @GetMapping({"/alt-text-feedback", "/alt-text-feedback/"})
    @Operation(summary = "AI 대체텍스트 피드백 목록 조회", description = "사용자가 남긴 AI 대체텍스트 좋아요/싫어요 평가 내역을 검색 및 필터링하여 페이징 조회합니다.")
    public ResponseEntity<AdminAltTextFeedbackPaginatedResponse> getAltTextFeedbacks(
            @RequestParam(required = false) String rating,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getAltTextFeedbacks(rating, search, page, size));
    }

    // ==========================================
    // 7. 서비스 이용 피드백 관리 (Service Feedback)
    // ==========================================

    @GetMapping({"/feedback", "/feedback/"})
    @Operation(summary = "서비스 피드백 목록 조회", description = "사용자가 남긴 서비스 만족도 별점(1~5점) 및 의견 피드백 목록을 조회 및 검색합니다.")
    public ResponseEntity<AdminFeedbackPaginatedResponse> getFeedbacks(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getFeedbacks(rating, search, page, size));
    }

    @DeleteMapping({"/feedback/{feedbackId}", "/feedback/{feedbackId}/"})
    @Operation(summary = "서비스 피드백 삭제", description = "관리자 권한으로 특정 서비스 피드백을 삭제합니다.")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long feedbackId) {
        adminService.deleteFeedback(feedbackId);
        return ResponseEntity.noContent().build();
    }
}
