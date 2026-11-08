package com.webridge.alttextfeedback.dto;

import java.time.OffsetDateTime;

public record AltTextResultWithFeedbackDto(
    Long id,
    String imgUrl,
    String elementHtml,
    String altText,
    String suggestedAlt,
    int compliance,
    String complianceDisplay,
    OffsetDateTime createdAt,
    String userFeedback,
    boolean hasAiSuggestion,
    String aiType,
    String aiGrade,
    String aiReason,
    String aiImprovement,
    boolean hasError
) {
    public static String getComplianceDisplay(int compliance) {
        return switch (compliance) {
            case 0 -> "문제 없음";
            case 1 -> "개선 필요";
            case 2 -> "미준수";
            case 3 -> "오류";
            default -> "";
        };
    }
}
