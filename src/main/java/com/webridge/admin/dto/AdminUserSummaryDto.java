package com.webridge.admin.dto;

import com.webridge.admin.util.SignupSurveyCodeMapper;
import com.webridge.user.entity.User;

import java.time.OffsetDateTime;

public record AdminUserSummaryDto(
    Long id,
    String email,
    String name,
    String provider,
    boolean isActive,
    boolean isAdmin,
    String pathToWebridge,
    String pathToWebridgeLabel,
    String occupation,
    String occupationLabel,
    OffsetDateTime lastLogin,
    OffsetDateTime createdAt,
    long scanCount
) {
    public static AdminUserSummaryDto from(User user, long scanCount) {
        return new AdminUserSummaryDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getProvider(),
            user.isActive(),
            user.isAdmin(),
            user.getPathToWebridge(),
            SignupSurveyCodeMapper.getPathLabel(user.getPathToWebridge(), user.getPathToWebridgeOther()),
            user.getOccupation(),
            SignupSurveyCodeMapper.getOccupationLabel(user.getOccupation(), user.getOccupationOther()),
            user.getLastLogin(),
            user.getCreatedAt(),
            scanCount
        );
    }
}
