package com.webridge.admin.dto;

import com.webridge.admin.util.SignupSurveyCodeMapper;
import com.webridge.user.entity.User;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminUserDetailDto(
    Long id,
    String email,
    String name,
    String provider,
    String providerId,
    boolean isActive,
    boolean isAdmin,
    boolean isSuperuser,
    // 유입경로 & 직군 설문 정보
    String pathToWebridge,
    String pathToWebridgeOther,
    String pathToWebridgeLabel,
    String occupation,
    String occupationOther,
    String occupationLabel,
    // 약관 동의 상태
    boolean serviceTermsAgreed,
    OffsetDateTime serviceTermsAgreedAt,
    boolean privacyPolicyAgreed,
    OffsetDateTime privacyPolicyAgreedAt,
    boolean marketingAgreed,
    OffsetDateTime marketingAgreedAt,
    // 타임스탬프 & 통계
    OffsetDateTime lastLogin,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    long totalScans,
    List<AdminScanSummaryDto> recentScans
) {
    public static AdminUserDetailDto from(User user, long totalScans, List<AdminScanSummaryDto> recentScans) {
        return new AdminUserDetailDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getProvider(),
            user.getProviderId(),
            user.isActive(),
            user.isAdmin(),
            user.isSuperuser(),
            user.getPathToWebridge(),
            user.getPathToWebridgeOther(),
            SignupSurveyCodeMapper.getPathLabel(user.getPathToWebridge(), user.getPathToWebridgeOther()),
            user.getOccupation(),
            user.getOccupationOther(),
            SignupSurveyCodeMapper.getOccupationLabel(user.getOccupation(), user.getOccupationOther()),
            user.isServiceTermsAgreed(),
            user.getServiceTermsAgreedAt(),
            user.isPrivacyPolicyAgreed(),
            user.getPrivacyPolicyAgreedAt(),
            user.isMarketingAgreed(),
            user.getMarketingAgreedAt(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            totalScans,
            recentScans
        );
    }
}
