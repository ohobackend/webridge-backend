package com.webridge.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserStatusUpdateRequest(
    @NotNull(message = "isActive 필드는 필수입니다.")
    Boolean isActive
) {}
