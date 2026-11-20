package com.webridge.admin.dto;

public record AdminLoginResponse(
    String accessToken,
    String refreshToken,
    AdminUserSummaryDto user,
    String message
) {}
