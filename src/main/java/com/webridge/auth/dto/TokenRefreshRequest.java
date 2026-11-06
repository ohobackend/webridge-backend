package com.webridge.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
    @NotBlank(message = "refresh 토큰을 입력해주세요.")
    String refresh
) {}
