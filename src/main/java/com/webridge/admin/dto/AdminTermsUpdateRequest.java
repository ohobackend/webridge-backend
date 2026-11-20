package com.webridge.admin.dto;

import jakarta.validation.constraints.Size;

public record AdminTermsUpdateRequest(
    @Size(max = 10)
    String version,

    @Size(max = 10)
    String changeType,

    Boolean requiresReagreement,

    @Size(max = 200)
    String changeSummary,

    String content,

    Boolean isActive
) {}
