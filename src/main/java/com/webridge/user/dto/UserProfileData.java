package com.webridge.user.dto;

import com.webridge.user.entity.User;

import java.time.OffsetDateTime;

public record UserProfileData(
    Long id,
    String email,
    String name,
    String provider,
    boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static UserProfileData from(User user) {
        return new UserProfileData(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getProvider(),
            user.isActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
