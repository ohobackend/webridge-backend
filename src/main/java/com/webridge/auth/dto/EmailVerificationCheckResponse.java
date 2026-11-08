package com.webridge.auth.dto;

public record EmailVerificationCheckResponse(
    String email,
    boolean verified,
    String message
) {}
