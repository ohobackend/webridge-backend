package com.webridge.scan.dto;

import com.webridge.scan.entity.ScanReport;

import java.time.OffsetDateTime;

public record ScanReportItemDto(
        String format,
        OffsetDateTime generatedAt,
        int downloadCount,
        String downloadUrl
) {
    public static ScanReportItemDto from(ScanReport report, String baseUrl) {
        if (report == null) return null;
        String downloadUrl = null;
        if (report.getFilePath() != null && !report.getFilePath().isBlank() && baseUrl != null) {
            downloadUrl = baseUrl + "/api/v1/scans/" + report.getScan().getId() + "/download-report/";
        }
        return new ScanReportItemDto(
                report.getFormat(),
                report.getGeneratedAt(),
                report.getDownloadCount(),
                downloadUrl
        );
    }
}
