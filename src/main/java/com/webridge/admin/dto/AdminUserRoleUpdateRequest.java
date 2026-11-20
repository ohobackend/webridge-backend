package com.webridge.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserRoleUpdateRequest(
    @NotNull(message = "isAdmin 필드는 필수입니다.")
    Boolean isAdmin
) {}
