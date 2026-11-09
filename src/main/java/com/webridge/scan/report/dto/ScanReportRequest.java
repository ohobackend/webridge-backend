package com.webridge.scan.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ScanReportRequest(
        @Schema(description = "리포트 포맷 (pdf, excel, json)", example = "pdf")
        String format
) {}
