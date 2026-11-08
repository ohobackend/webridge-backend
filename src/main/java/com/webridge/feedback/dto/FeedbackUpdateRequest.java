package com.webridge.feedback.dto;

public record FeedbackUpdateRequest(
    String content,
    Integer rating
) {}
