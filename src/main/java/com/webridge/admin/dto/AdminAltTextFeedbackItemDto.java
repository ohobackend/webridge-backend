package com.webridge.admin.dto;

import com.webridge.alttextfeedback.entity.AltTextFeedback;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.AltTextResult;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminAltTextFeedbackItemDto(
    Long id,
    String rating,
    OffsetDateTime createdAt,
    Long altTextResultId,
    String imgUrl,
    String altText,
    String suggestedAlt,
    String aiImprovement,
    UUID scanId,
    String scanUrl,
    String scanTitle,
    String userEmail
) {
    public static AdminAltTextFeedbackItemDto from(AltTextFeedback feedback) {
        AltTextResult result = feedback.getAltTextResult();
        AccessibilityScan scan = result != null ? result.getScan() : null;

        return new AdminAltTextFeedbackItemDto(
            feedback.getId(),
            feedback.getRating(),
            feedback.getCreatedAt(),
            result != null ? result.getId() : null,
            result != null ? result.getImgUrl() : null,
            result != null ? result.getAltText() : null,
            result != null ? result.getSuggestedAlt() : null,
            result != null ? result.getAiImprovement() : null,
            scan != null ? scan.getId() : null,
            scan != null ? scan.getUrl() : null,
            scan != null ? scan.getTitle() : null,
            feedback.getUser() != null ? feedback.getUser().getEmail() : null
        );
    }
}
