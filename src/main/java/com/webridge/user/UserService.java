package com.webridge.user;

import com.webridge.common.exception.ApiException;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.user.dto.UserDeleteResponse;
import com.webridge.user.dto.UserExtraInfoRequest;
import com.webridge.user.dto.UserExtraInfoResponse;
import com.webridge.user.dto.UserProfileData;
import com.webridge.user.dto.UserProfileResponse;
import com.webridge.user.dto.UserUpdateRequest;
import com.webridge.user.entity.User;
import com.webridge.user.repository.EmailVerificationRepository;
import com.webridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AccessibilityScanRepository scanRepository;
    private final EmailVerificationRepository emailVerificationRepository;

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

        List<AccessibilityScan> activeScans = scanRepository.findByUserIdAndStatus(userId, ScanStatus.PROCESSING);
        for (AccessibilityScan scan : activeScans) {
            scan.setStatus(ScanStatus.CANCELLED);
            scan.setErrorMessage("사용자 탈퇴로 인한 취소");
        }
        scanRepository.deleteAll(activeScans);

        emailVerificationRepository.deleteByEmail(userEmail);
        userRepository.delete(user);

        return new UserDeleteResponse(
            true,
            userEmail + " 계정이 성공적으로 탈퇴되었습니다.",
            Map.of(
                "deleted_email", userEmail,
                "deleted_at", OffsetDateTime.now().toString()
            )
        );
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
