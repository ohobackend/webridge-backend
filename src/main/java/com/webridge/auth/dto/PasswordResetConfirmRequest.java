package com.webridge.auth.dto;

public record PasswordResetConfirmRequest(
    String uid,
    String token,
    String newPassword
) {}
