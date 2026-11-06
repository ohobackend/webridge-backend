package com.webridge.auth.dto;

import java.util.Map;

public record AuthResponse(
    UserDto user,
    String access,
    String refresh,
    Map<String, String> tokens,
    String message
) {
    public static AuthResponse of(UserDto user, String access, String refresh, String message) {
        return new AuthResponse(
            user,
            access,
            refresh,
            Map.of("access", access, "refresh", refresh),
            message
        );
    }
}
