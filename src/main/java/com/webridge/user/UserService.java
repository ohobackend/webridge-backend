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

        // 4. 사용자 본인 삭제
        userRepository.delete(user);
        userRepository.flush();

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
        if (!tableExists("accessibility_scans")) {
            return;
        }

        String scanSubquery = "SELECT id FROM accessibility_scans WHERE user_id = ?";

        // 1-1. 스캔 결과에 달린 alt_text_feedbacks 삭제
        if (tableExists("alt_text_feedbacks") && tableExists("alt_text_results")) {
            safeDelete("DELETE FROM alt_text_feedbacks WHERE alt_text_result_id IN (SELECT id FROM alt_text_results WHERE scan_id IN (" + scanSubquery + "))", userId);
        }

        // 1-2. 13개 검사 결과 테이블 데이터 삭제
        String[] resultTables = {
            "alt_text_results", "contrast_results", "keyboard_results", "label_results",
            "table_results", "video_caption_results", "video_autoplay_results", "basic_language_results",
            "markup_error_results", "heading_results", "response_time_results", "pause_control_results",
            "flashing_results", "scan_reports"
        };
        for (String table : resultTables) {
            if (tableExists(table)) {
                safeDelete("DELETE FROM " + table + " WHERE scan_id IN (" + scanSubquery + ")", userId);
            }
        }

        // 1-3. 스캔 레코드 삭제
        safeDelete("DELETE FROM accessibility_scans WHERE user_id = ?", userId);
    }

    private void deleteUserUserData(Long userId, String email) {
        if (tableExists("alt_text_feedbacks")) {
            safeDelete("DELETE FROM alt_text_feedbacks WHERE user_id = ?", userId);
        }
        if (tableExists("feedbacks")) {
            safeDelete("DELETE FROM feedbacks WHERE user_id = ?", userId);
        }
        if (tableExists("survey_triggers")) {
            safeDelete("DELETE FROM survey_triggers WHERE user_id = ?", userId);
        }
        if (tableExists("surveys")) {
            safeDelete("DELETE FROM surveys WHERE user_id = ?", userId);
        }
        if (tableExists("social_accounts")) {
            safeDelete("DELETE FROM social_accounts WHERE user_id = ?", userId);
        }
        if (email != null && !email.isBlank() && tableExists("email_verifications")) {
            safeDelete("DELETE FROM email_verifications WHERE email = ?", email);
        }
    }

    private void deleteLegacyDjangoData(Long userId) {
        if (tableExists("token_blacklist_blacklistedtoken") && tableExists("token_blacklist_outstandingtoken")) {
            safeDelete("DELETE FROM token_blacklist_blacklistedtoken WHERE token_id IN (SELECT id FROM token_blacklist_outstandingtoken WHERE user_id = ?)", userId);
        }
        if (tableExists("token_blacklist_outstandingtoken")) {
            safeDelete("DELETE FROM token_blacklist_outstandingtoken WHERE user_id = ?", userId);
        }
        if (tableExists("core_user_groups")) {
            safeDelete("DELETE FROM core_user_groups WHERE user_id = ?", userId);
        }
        if (tableExists("core_user_user_permissions")) {
            safeDelete("DELETE FROM core_user_user_permissions WHERE user_id = ?", userId);
        }
        if (tableExists("django_admin_log")) {
            safeDelete("DELETE FROM django_admin_log WHERE user_id = ?", userId);
        }
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = LOWER(?)",
                Integer.class,
                tableName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("tableExists check failed for '{}': {}", tableName, e.getMessage());
            return false;
        }
    }

    private void safeDelete(String sql, Object... args) {
        try {
            jdbcTemplate.update(sql, args);
        } catch (Exception e) {
            log.warn("safeDelete error for sql '{}': {}", sql, e.getMessage());
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
