package com.webridge.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminTermsCreateRequest(
    @NotBlank(message = "약관 유형은 필수입니다. (service, privacy, marketing 등)")
    @Size(max = 20)
    String type,

    @NotBlank(message = "버전은 필수입니다. (예: 1.1, 2.0)")
    @Size(max = 10)
    String version,

    @NotBlank(message = "변경 유형은 필수입니다. (major, minor, patch)")
    @Size(max = 10)
    String changeType,

    Boolean requiresReagreement,

    @Size(max = 200)
    String changeSummary,

    @NotBlank(message = "약관 내용은 필수입니다.")
    String content,

    Boolean isActive
) {}
