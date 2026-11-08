package com.webridge.alttextfeedback.dto;

import com.webridge.alttextfeedback.entity.AltTextFeedback;

import java.time.OffsetDateTime;

public record AltTextFeedbackDto(
    Long id,
    Long altTextResult,
    String rating,
    OffsetDateTime createdAt
) {
    public static AltTextFeedbackDto from(AltTextFeedback feedback) {
        return new AltTextFeedbackDto(
            feedback.getId(),
            feedback.getAltTextResult().getId(),
            feedback.getRating(),
            feedback.getCreatedAt()
        );
    }
}
