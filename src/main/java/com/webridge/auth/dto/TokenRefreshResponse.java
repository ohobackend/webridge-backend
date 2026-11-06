package com.webridge.auth.dto;

public record TokenRefreshResponse(
    String access,
    String refresh
) {}
