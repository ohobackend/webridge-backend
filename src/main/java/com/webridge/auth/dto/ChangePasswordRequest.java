package com.webridge.auth.dto;

public record ChangePasswordRequest(
    String newPassword,
    String newPasswordConfirm
) {}
