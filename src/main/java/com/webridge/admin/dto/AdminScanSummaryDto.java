package com.webridge.admin.dto;

import com.webridge.scan.entity.AccessibilityScan;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminScanSummaryDto(
    UUID id,
    String url,
    String title,
    String status,
    String statusDisplay,
    int progress,
    int totalIssues,
    double complianceScore,
    UserBriefDto user,
    OffsetDateTime createdAt,
    OffsetDateTime completedAt
) {
    public record UserBriefDto(
        Long id,
        String email,
        String name
    ) {}

    public static AdminScanSummaryDto from(AccessibilityScan scan) {
        UserBriefDto userBrief = null;
        if (scan.getUser() != null) {
            userBrief = new UserBriefDto(
                scan.getUser().getId(),
                scan.getUser().getEmail(),
                scan.getUser().getName()
            );
        }

        String statusValue = scan.getStatus() != null ? scan.getStatus().getValue() : "pending";
        String statusDisplay = switch (statusValue) {
            case "pending" -> "대기중";
            case "processing" -> "진행중";
            case "completed" -> "완료";
            case "failed" -> "실패";
            case "cancelled" -> "취소";
            default -> statusValue;
        };

        return new AdminScanSummaryDto(
            scan.getId(),
            scan.getUrl(),
            scan.getTitle() != null ? scan.getTitle() : "",
            statusValue,
            statusDisplay,
            scan.getProgress(),
            scan.getTotalIssues(),
            scan.getComplianceScore(),
            userBrief,
            scan.getCreatedAt(),
            scan.getCompletedAt()
        );
    }
}
