package com.webridge.user;

import com.webridge.common.exception.ApiException;
import com.webridge.user.dto.UserDeleteResponse;
import com.webridge.user.dto.UserExtraInfoRequest;
import com.webridge.user.dto.UserExtraInfoResponse;
import com.webridge.user.dto.UserProfileData;
import com.webridge.user.dto.UserProfileResponse;
import com.webridge.user.dto.UserUpdateRequest;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = getUserOrThrow(userId);
        return new UserProfileResponse(
            true,
            "사용자 정보를 성공적으로 조회했습니다.",
            UserProfileData.from(user)
        );
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserUpdateRequest req) {
        User user = getUserOrThrow(userId);

        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of(
                "success", false,
                "message", "입력 데이터가 유효하지 않습니다.",
                "errors", Map.of("name", List.of("이름은 필수 항목입니다."))
            ));
        }

        String trimmedName = req.name().trim();
        if (trimmedName.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of(
                "success", false,
                "message", "입력 데이터가 유효하지 않습니다.",
                "errors", Map.of("name", List.of("이름은 최소 2자 이상이어야 합니다."))
            ));
        }

        user.setName(trimmedName);
        userRepository.save(user);

        return new UserProfileResponse(
            true,
            "사용자 정보가 성공적으로 수정되었습니다.",
            UserProfileData.from(user)
        );
    }

    @Transactional
    public UserDeleteResponse deleteUser(Long userId) {
        User user = getUserOrThrow(userId);
        String userEmail = user.getEmail();

        // 1. 사용자 스캔 및 하위 검사 결과/보고서 전체 삭제
        deleteUserScansAndResults(userId);

        // 2. 사용자 피드백, 설문, 소셜 계정, 인증 정보 삭제
        deleteUserUserData(userId, userEmail);

        // 3. Django 레거시 테이블 데이터 정리 (존재할 경우)
        deleteLegacyDjangoData(userId);

        // 영속성 컨텍스트 초기화 후 사용자 삭제
        entityManager.clear();
        userRepository.deleteById(userId);

        log.info("User {} ({}) successfully deleted with all cascaded relations", userId, userEmail);

        return new UserDeleteResponse(
            true,
            userEmail + " 계정이 성공적으로 탈퇴되었습니다.",
            Map.of(
                "deleted_email", userEmail,
                "deleted_at", OffsetDateTime.now().toString()
            )
        );
    }

    private void deleteUserScansAndResults(Long userId) {
        String scanSubquery = "SELECT id FROM accessibility_scans WHERE user_id = ?";

        // 1-1. 스캔 결과에 달린 alt_text_feedbacks 삭제
        safeDelete("DELETE FROM alt_text_feedbacks WHERE alt_text_result_id IN (SELECT id FROM alt_text_results WHERE scan_id IN (" + scanSubquery + "))", userId);

        // 1-2. 13개 검사 결과 테이블 데이터 삭제
        safeDelete("DELETE FROM alt_text_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM contrast_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM keyboard_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM label_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM table_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM video_caption_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM video_autoplay_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM basic_language_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM markup_error_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM heading_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM response_time_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM pause_control_results WHERE scan_id IN (" + scanSubquery + ")", userId);
        safeDelete("DELETE FROM flashing_results WHERE scan_id IN (" + scanSubquery + ")", userId);

        // 1-3. 스캔 보고서 삭제
        safeDelete("DELETE FROM scan_reports WHERE scan_id IN (" + scanSubquery + ")", userId);

        // 1-4. 스캔 레코드 삭제
        safeDelete("DELETE FROM accessibility_scans WHERE user_id = ?", userId);
    }

    private void deleteUserUserData(Long userId, String email) {
        safeDelete("DELETE FROM alt_text_feedbacks WHERE user_id = ?", userId);
        safeDelete("DELETE FROM feedbacks WHERE user_id = ?", userId);
        safeDelete("DELETE FROM survey_triggers WHERE user_id = ?", userId);
        safeDelete("DELETE FROM surveys WHERE user_id = ?", userId);
        safeDelete("DELETE FROM social_accounts WHERE user_id = ?", userId);
        if (email != null && !email.isBlank()) {
            safeDelete("DELETE FROM email_verifications WHERE email = ?", email);
        }
    }

    private void deleteLegacyDjangoData(Long userId) {
        safeDelete("DELETE FROM token_blacklist_blacklistedtoken WHERE token_id IN (SELECT id FROM token_blacklist_outstandingtoken WHERE user_id = ?)", userId);
        safeDelete("DELETE FROM token_blacklist_outstandingtoken WHERE user_id = ?", userId);
        safeDelete("DELETE FROM core_user_groups WHERE user_id = ?", userId);
        safeDelete("DELETE FROM core_user_user_permissions WHERE user_id = ?", userId);
        safeDelete("DELETE FROM django_admin_log WHERE user_id = ?", userId);
    }

    private void safeDelete(String sql, Object... args) {
        try {
            jdbcTemplate.update(sql, args);
        } catch (Exception e) {
            log.debug("safeDelete ignored error for sql '{}': {}", sql, e.getMessage());
        }
    }

    @Transactional
    public UserExtraInfoResponse updateExtraInfo(Long userId, UserExtraInfoRequest req) {
        User user = getUserOrThrow(userId);

        Map<String, List<String>> errors = new LinkedHashMap<>();

        if ("f".equals(req.pathToWebridge()) && (req.pathToWebridgeOther() == null || req.pathToWebridgeOther().isBlank())) {
            errors.put("path_to_webridge_other", List.of("기타 선택 시 필수 입력입니다."));
        }

        if ("g".equals(req.occupation()) && (req.occupationOther() == null || req.occupationOther().isBlank())) {
            errors.put("occupation_other", List.of("기타 선택 시 필수 입력입니다."));
        }

        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, errors);
        }

        if (req.pathToWebridge() != null) {
            user.setPathToWebridge(req.pathToWebridge());
            user.setPathToWebridgeOther("f".equals(req.pathToWebridge()) ? req.pathToWebridgeOther() : null);
        }

        if (req.occupation() != null) {
            user.setOccupation(req.occupation());
            user.setOccupationOther("g".equals(req.occupation()) ? req.occupationOther() : null);
        }

        userRepository.save(user);

        return UserExtraInfoResponse.from(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));
    }
}
