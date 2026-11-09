package com.webridge.scan.dto;

import com.webridge.scan.entity.AltTextResult;

import java.time.OffsetDateTime;

public record AltTextItemDto(
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
    public static AltTextItemDto from(AltTextResult r, String userFeedback) {
        String display = switch (r.getCompliance()) {
            case 0 -> "문제 없음";
            case 1 -> "개선 필요";
            case 2 -> "미준수";
            default -> "오류";
        };
        boolean hasAi = r.getSuggestedAlt() != null && !r.getSuggestedAlt().isBlank();

        return new AltTextItemDto(
                r.getId(),
                r.getImgUrl(),
                r.getElementHtml(),
                r.getAltText(),
                r.getSuggestedAlt(),
                r.getCompliance(),
                display,
                r.getCreatedAt(),
                userFeedback,
                hasAi,
                r.getAiType(),
                r.getAiGrade(),
                r.getAiReason(),
                r.getAiImprovement(),
                r.isHasError()
        );
    }
}
