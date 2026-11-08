package com.webridge.auth.dto;

public record SocialLoginRequest(
    String provider,
    String accessToken
) {}
