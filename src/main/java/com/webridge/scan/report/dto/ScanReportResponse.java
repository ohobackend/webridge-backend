package com.webridge.scan.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ScanReportResponse(
        @Schema(description = "리포트 생성 작업 ID")
        String taskId,

        @Schema(description = "상태 메시지")
        String message,

        @Schema(description = "리포트 포맷")
        String format
) {}
