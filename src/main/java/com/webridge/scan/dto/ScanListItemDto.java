package com.webridge.scan.dto;

import com.webridge.scan.entity.AccessibilityScan;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScanListItemDto(
        UUID id,
        String url,
        String title,
        String status,
        String statusDisplay,
        int totalIssues,
        double complianceScore,
        int completionPercentage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime completedAt
) {
    public static ScanListItemDto from(AccessibilityScan scan) {
        String statusVal = scan.getStatus() != null ? scan.getStatus().getValue() : "pending";
        String statusDisplay = switch (statusVal) {
            case "processing" -> "진행중";
            case "completed" -> "완료";
            case "failed" -> "실패";
            case "cancelled" -> "취소";
            default -> "대기중";
        };

        return new ScanListItemDto(
                scan.getId(),
                scan.getUrl(),
                scan.getTitle() != null ? scan.getTitle() : "",
                statusVal,
                statusDisplay,
                scan.getTotalIssues(),
                scan.getComplianceScore(),
                (int) Math.round(scan.getCompletionPercentage()),
                scan.getCreatedAt(),
                scan.getUpdatedAt(),
                scan.getCompletedAt()
        );
    }
}
