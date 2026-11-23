package com.webridge.admin.dto;

import java.util.List;

public record AdminAltTextFeedbackPaginatedResponse(
    long count,
    String next,
    String previous,
    long totalLikes,
    long totalDislikes,
    List<AdminAltTextFeedbackItemDto> results
) {}
