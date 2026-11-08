package com.webridge.feedback.dto;

import com.webridge.feedback.entity.Feedback;

import java.time.OffsetDateTime;

public record FeedbackDto(
    Long id,
    String content,
    int rating,
    OffsetDateTime createdAt
) {
    public static FeedbackDto from(Feedback feedback) {
        return new FeedbackDto(
            feedback.getId(),
            feedback.getContent(),
            feedback.getRating(),
            feedback.getCreatedAt()
        );
    }
}
