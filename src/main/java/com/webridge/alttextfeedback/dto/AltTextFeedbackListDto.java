package com.webridge.alttextfeedback.dto;

import com.webridge.alttextfeedback.entity.AltTextFeedback;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AltTextFeedbackListDto(
    Long id,
    String rating,
    OffsetDateTime createdAt,
    String suggestedAlt,
    String imgUrl,
    UUID scanId,
    String scanUrl,
    String userEmail
) {
    public static AltTextFeedbackListDto from(AltTextFeedback feedback) {
        return new AltTextFeedbackListDto(
            feedback.getId(),
            feedback.getRating(),
            feedback.getCreatedAt(),
            feedback.getAltTextResult() != null ? feedback.getAltTextResult().getSuggestedAlt() : "",
            feedback.getAltTextResult() != null ? feedback.getAltTextResult().getImgUrl() : "",
            feedback.getAltTextResult() != null && feedback.getAltTextResult().getScan() != null
                ? feedback.getAltTextResult().getScan().getId() : null,
            feedback.getAltTextResult() != null && feedback.getAltTextResult().getScan() != null
                ? feedback.getAltTextResult().getScan().getUrl() : "",
            feedback.getUser() != null ? feedback.getUser().getEmail() : "탈퇴한 사용자"
        );
    }
}
