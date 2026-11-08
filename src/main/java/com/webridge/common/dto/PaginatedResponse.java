package com.webridge.common.dto;

import java.util.List;

public record PaginatedResponse<T>(
    long count,
    String next,
    String previous,
    List<T> results
) {}
