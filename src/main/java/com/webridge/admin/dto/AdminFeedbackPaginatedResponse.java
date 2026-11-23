package com.webridge.admin.dto;

import java.util.List;
import java.util.Map;

public record AdminFeedbackPaginatedResponse(
    long count,
    String next,
    String previous,
    double averageRating,
    Map<String, Long> ratingCounts,
    List<AdminFeedbackItemDto> results
) {}
