package com.webridge.feedback.dto;

public record FeedbackCreateRequest(
    String content,
    Integer rating
) {}
