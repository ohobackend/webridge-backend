package com.webridge.user.dto;

public record UserProfileResponse(
    boolean success,
    String message,
    UserProfileData data
) {}
