package com.webridge.user.dto;

import java.util.Map;

public record UserDeleteResponse(
    boolean success,
    String message,
    Map<String, String> data
) {}
