package com.webridge.admin.dto;

import com.webridge.feedback.entity.Feedback;
import com.webridge.user.entity.User;

import java.time.OffsetDateTime;

public record AdminFeedbackItemDto(
    Long id,
    int rating,
    String content,
    Long userId,
    String userEmail,
    String userName,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static AdminFeedbackItemDto from(Feedback feedback) {
        User user = feedback.getUser();
        return new AdminFeedbackItemDto(
            feedback.getId(),
            feedback.getRating(),
            feedback.getContent(),
            user != null ? user.getId() : null,
            user != null ? user.getEmail() : null,
            user != null ? user.getName() : null,
            feedback.getCreatedAt(),
            feedback.getUpdatedAt()
        );
    }
}
