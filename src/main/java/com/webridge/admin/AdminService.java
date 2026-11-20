package com.webridge.admin;

import com.webridge.admin.dto.*;
import com.webridge.admin.util.SignupSurveyCodeMapper;
import com.webridge.common.dto.PaginatedResponse;
import com.webridge.common.exception.ApiException;
import com.webridge.scan.dto.ScanDetailDto;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanReport;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.scan.repository.ScanReportRepository;
import com.webridge.terms.entity.Terms;
import com.webridge.terms.repository.TermsRepository;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.webridge.auth.jwt.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final AccessibilityScanRepository scanRepository;
    private final ScanReportRepository reportRepository;
    private final TermsRepository termsRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // ==========================================
    // 0. 관리자 인증 (Admin Authentication)
    // ==========================================

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest req) {
        if (req == null || req.username() == null || req.username().isBlank() || req.password() == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "아이디와 비밀번호를 모두 입력해주세요.");
        }

        String username = req.username().trim();
        String password = req.password();

        // inableit 또는 inableit@inableit.com 또는 일반 이메일로 검색
        User user = userRepository.findByEmail(username)
            .or(() -> userRepository.findByEmail(username + "@inableit.com"))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "관리자 권한이 없는 계정입니다.");
        }

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "비활성화된 관리자 계정입니다.");
        }

        user.setLastLogin(OffsetDateTime.now());
        userRepository.save(user);

        String access = jwtProvider.createAccess(user.getId());
        String refresh = jwtProvider.createRefresh(user.getId());

        long scanCount = scanRepository.countByUserId(user.getId());
        AdminUserSummaryDto userSummary = AdminUserSummaryDto.from(user, scanCount);

        return new AdminLoginResponse(access, refresh, userSummary, "관리자로 성공적으로 로그인되었습니다.");
    }

    // ==========================================
    // 1. 회원 관리 (User Management)
    // ==========================================

    @Transactional(readOnly = true)
    public PaginatedResponse<AdminUserSummaryDto> getUsers(
            String search, Boolean isAdmin, Boolean isActive, String provider, int page, int size) {

        int pageIndex = Math.max(0, page - 1);
        int pageSize = (size > 0) ? Math.min(size, 100) : 20;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String searchKeyword = (search != null && !search.isBlank()) ? search.trim() : null;
        String providerFilter = (provider != null && !provider.isBlank()) ? provider.trim() : null;

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchKeyword != null) {
                String pattern = "%" + searchKeyword.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            if (isAdmin != null) {
                predicates.add(cb.equal(root.get("isAdmin"), isAdmin));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (providerFilter != null) {
                predicates.add(cb.equal(cb.lower(root.get("provider")), providerFilter.toLowerCase()));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<AdminUserSummaryDto> dtos = userPage.getContent().stream()
            .map(u -> {
                long scanCount = scanRepository.countByUserId(u.getId());
                return AdminUserSummaryDto.from(u, scanCount);
            })
            .toList();

        return new PaginatedResponse<>(
            userPage.getTotalElements(),
            userPage.hasNext() ? String.valueOf(page + 1) : null,
            userPage.hasPrevious() ? String.valueOf(page - 1) : null,
            dtos
        );
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));

        long totalScans = scanRepository.countByUserId(userId);
        List<AdminScanSummaryDto> recentScans = scanRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(AdminScanSummaryDto::from)
            .toList();

        return AdminUserDetailDto.from(user, totalScans, recentScans);
    }

    @Transactional
    public AdminUserSummaryDto updateUserStatus(Long userId, boolean isActive) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));

        user.setActive(isActive);
        userRepository.save(user);

        long scanCount = scanRepository.countByUserId(user.getId());
        return AdminUserSummaryDto.from(user, scanCount);
    }

    @Transactional
    public AdminUserSummaryDto updateUserRole(Long userId, boolean isAdmin) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));

        user.setAdmin(isAdmin);
        userRepository.save(user);

        long scanCount = scanRepository.countByUserId(user.getId());
        return AdminUserSummaryDto.from(user, scanCount);
    }

    // ==========================================
    // 2. 검사 내역 관리 (Scan Management)
    // ==========================================

    @Transactional(readOnly = true)
    public PaginatedResponse<AdminScanSummaryDto> getScans(String search, String status, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = (size > 0) ? Math.min(size, 100) : 20;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String searchKeyword = (search != null && !search.isBlank()) ? search.trim() : null;
        ScanStatus scanStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                scanStatus = ScanStatus.fromValue(status.trim().toLowerCase());
            } catch (Exception ignored) {}
        }
        final ScanStatus targetStatus = scanStatus;

        Specification<AccessibilityScan> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchKeyword != null) {
                String pattern = "%" + searchKeyword.toLowerCase() + "%";
                Join<AccessibilityScan, User> userJoin = root.join("user", JoinType.LEFT);
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("url")), pattern),
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(userJoin.get("email")), pattern)
                ));
            }

            if (targetStatus != null) {
                predicates.add(cb.equal(root.get("status"), targetStatus));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AccessibilityScan> scanPage = scanRepository.findAll(spec, pageable);

        List<AdminScanSummaryDto> dtos = scanPage.getContent().stream()
            .map(AdminScanSummaryDto::from)
            .toList();

        return new PaginatedResponse<>(
            scanPage.getTotalElements(),
            scanPage.hasNext() ? String.valueOf(page + 1) : null,
            scanPage.hasPrevious() ? String.valueOf(page - 1) : null,
            dtos
        );
    }

    @Transactional(readOnly = true)
    public ScanDetailDto getScanDetail(UUID scanId) {
        AccessibilityScan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "검사 기록을 찾을 수 없습니다.")));

        ScanReport report = reportRepository.findByScanId(scanId).orElse(null);
        return ScanDetailDto.from(scan, Map.of(), "", report);
    }

    @Transactional
    public void deleteScan(UUID scanId) {
        AccessibilityScan scan = scanRepository.findById(scanId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "검사 기록을 찾을 수 없습니다.")));

        scanRepository.delete(scan);
    }

    // ==========================================
    // 3. 약관 관리 (Terms Management)
    // ==========================================

    @Transactional(readOnly = true)
    public List<AdminTermsDto> getAllTerms() {
        return termsRepository.findAll(Sort.by(Sort.Direction.ASC, "type")).stream()
            .map(AdminTermsDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminTermsDto getTermsDetail(Long termsId) {
        Terms terms = termsRepository.findById(termsId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "해당 약관을 찾을 수 없습니다.")));

        return AdminTermsDto.from(terms);
    }

    @Transactional
    public AdminTermsDto createTerms(AdminTermsCreateRequest request) {
        if (termsRepository.findByType(request.type()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("type", List.of("이미 존재하는 약관 유형입니다.")));
        }

        Terms terms = Terms.builder()
            .type(request.type().trim().toLowerCase())
            .version(request.version().trim())
            .changeType(request.changeType().trim().toLowerCase())
            .requiresReagreement(request.requiresReagreement() == null || request.requiresReagreement())
            .changeSummary(request.changeSummary() != null ? request.changeSummary().trim() : "")
            .content(request.content())
            .isActive(request.isActive() == null || request.isActive())
            .build();

        termsRepository.save(terms);
        return AdminTermsDto.from(terms);
    }

    @Transactional
    public AdminTermsDto updateTerms(Long termsId, AdminTermsUpdateRequest request) {
        Terms terms = termsRepository.findById(termsId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "해당 약관을 찾을 수 없습니다.")));

        if (request.version() != null && !request.version().isBlank()) {
            terms.setVersion(request.version().trim());
        }
        if (request.changeType() != null && !request.changeType().isBlank()) {
            terms.setChangeType(request.changeType().trim().toLowerCase());
        }
        if (request.requiresReagreement() != null) {
            terms.setRequiresReagreement(request.requiresReagreement());
        }
        if (request.changeSummary() != null) {
            terms.setChangeSummary(request.changeSummary().trim());
        }
        if (request.content() != null && !request.content().isBlank()) {
            terms.setContent(request.content());
        }
        if (request.isActive() != null) {
            terms.setActive(request.isActive());
        }

        termsRepository.save(terms);
        return AdminTermsDto.from(terms);
    }

    // ==========================================
    // 4. 대시보드 통계 (Dashboard Stats)
    // ==========================================

    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getDashboardStats() {
        OffsetDateTime todayStart = LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime();

        // 1. 사용자 카운트
        long totalUsers = userRepository.count();
        long todayNewUsers = userRepository.countByCreatedAtAfter(todayStart);
        long activeUsers = userRepository.countByIsActiveTrue();
        long adminUsers = userRepository.countByIsAdminTrue();

        // 2. 검사 카운트
        long totalScans = scanRepository.count();
        long todayScans = scanRepository.countByCreatedAtAfter(todayStart);
        long completedScans = scanRepository.countByStatus(ScanStatus.COMPLETED);
        long failedScans = scanRepository.countByStatus(ScanStatus.FAILED);
        Double avgScore = scanRepository.findAverageComplianceScore();
        double averageComplianceScore = avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0.0;

        // 3. 최근 7일 일별 신규 가입자 & 검사 추이
        List<AdminDailyStatDto> dailyNewUsers = calculateDailyUserStats(7);
        List<AdminDailyStatDto> dailyScans = calculateDailyScanStats(7);

        // 4. 최근 목록 (각 5건)
        List<AdminScanSummaryDto> recentScans = scanRepository.findTop5ByOrderByCreatedAtDesc().stream()
            .map(AdminScanSummaryDto::from)
            .toList();

        List<AdminUserSummaryDto> recentUsers = userRepository.findTop5ByOrderByCreatedAtDesc().stream()
            .map(u -> AdminUserSummaryDto.from(u, scanRepository.countByUserId(u.getId())))
            .toList();

        return new AdminDashboardStatsDto(
            totalUsers,
            todayNewUsers,
            activeUsers,
            adminUsers,
            totalScans,
            todayScans,
            completedScans,
            failedScans,
            averageComplianceScore,
            dailyNewUsers,
            dailyScans,
            recentScans,
            recentUsers
        );
    }

    // ==========================================
    // 5. 가입 설문조사 통계 (유입경로 & 직군)
    // ==========================================

    @Transactional(readOnly = true)
    public AdminSignupSurveyStatsDto getSignupSurveyStats() {
        List<User> allUsers = userRepository.findAll();
        long totalUsers = allUsers.size();

        List<User> respondedUsers = allUsers.stream()
            .filter(u -> (u.getPathToWebridge() != null && !u.getPathToWebridge().isBlank())
                      || (u.getOccupation() != null && !u.getOccupation().isBlank()))
            .toList();

        long totalResponded = respondedUsers.size();
        double responseRate = totalUsers > 0
            ? Math.round(((double) totalResponded / totalUsers * 100.0) * 10.0) / 10.0
            : 0.0;

        // 1. 유입경로 통계
        Map<String, String> pathChoices = SignupSurveyCodeMapper.getAllPathChoices();
        Map<String, Long> pathCounts = allUsers.stream()
            .filter(u -> u.getPathToWebridge() != null && !u.getPathToWebridge().isBlank())
            .collect(Collectors.groupingBy(u -> u.getPathToWebridge().toLowerCase(), Collectors.counting()));

        List<AdminSignupSurveyStatsDto.CategoryStatDto> pathStats = new ArrayList<>();
        pathChoices.forEach((code, label) -> {
            long count = pathCounts.getOrDefault(code, 0L);
            double pct = totalResponded > 0 ? Math.round(((double) count / totalResponded * 100.0) * 10.0) / 10.0 : 0.0;
            pathStats.add(new AdminSignupSurveyStatsDto.CategoryStatDto(code, label, count, pct));
        });

        // 2. 직군 통계
        Map<String, String> occupationChoices = SignupSurveyCodeMapper.getAllOccupationChoices();
        Map<String, Long> occCounts = allUsers.stream()
            .filter(u -> u.getOccupation() != null && !u.getOccupation().isBlank())
            .collect(Collectors.groupingBy(u -> u.getOccupation().toLowerCase(), Collectors.counting()));

        List<AdminSignupSurveyStatsDto.CategoryStatDto> occStats = new ArrayList<>();
        occupationChoices.forEach((code, label) -> {
            long count = occCounts.getOrDefault(code, 0L);
            double pct = totalResponded > 0 ? Math.round(((double) count / totalResponded * 100.0) * 10.0) / 10.0 : 0.0;
            occStats.add(new AdminSignupSurveyStatsDto.CategoryStatDto(code, label, count, pct));
        });

        // 3. 최근 기타 직접 입력 응답들
        List<String> recentPathOthers = allUsers.stream()
            .map(User::getPathToWebridgeOther)
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .limit(10)
            .toList();

        List<String> recentOccOthers = allUsers.stream()
            .map(User::getOccupationOther)
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .limit(10)
            .toList();

        return new AdminSignupSurveyStatsDto(
            totalResponded,
            totalUsers,
            responseRate,
            pathStats,
            occStats,
            recentPathOthers,
            recentOccOthers
        );
    }

    // ==========================================
    // 헬퍼 메서드
    // ==========================================

    private List<AdminDailyStatDto> calculateDailyUserStats(int days) {
        LocalDate today = LocalDate.now(KST);
        OffsetDateTime since = today.minusDays(days - 1).atStartOfDay(KST).toOffsetDateTime();
        List<User> recentUsers = userRepository.findAll().stream()
            .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(since))
            .toList();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<AdminDailyStatDto> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            String dateStr = targetDate.format(fmt);
            long count = recentUsers.stream()
                .filter(u -> targetDate.equals(u.getCreatedAt().atZoneSameInstant(KST).toLocalDate()))
                .count();
            result.add(new AdminDailyStatDto(dateStr, count));
        }
        return result;
    }

    private List<AdminDailyStatDto> calculateDailyScanStats(int days) {
        LocalDate today = LocalDate.now(KST);
        OffsetDateTime since = today.minusDays(days - 1).atStartOfDay(KST).toOffsetDateTime();
        List<AccessibilityScan> recentScans = scanRepository.findAll().stream()
            .filter(s -> s.getCreatedAt() != null && !s.getCreatedAt().isBefore(since))
            .toList();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<AdminDailyStatDto> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            String dateStr = targetDate.format(fmt);
            long count = recentScans.stream()
                .filter(s -> targetDate.equals(s.getCreatedAt().atZoneSameInstant(KST).toLocalDate()))
                .count();
            result.add(new AdminDailyStatDto(dateStr, count));
        }
        return result;
    }
}
