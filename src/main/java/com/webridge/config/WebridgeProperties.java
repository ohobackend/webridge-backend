package com.webridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webridge")
public record WebridgeProperties(
    Jwt jwt,
    String corsAllowedOrigins,
    String frontendUrl,
    String backendUrl,
    String openaiApiKey,
    String reportsDir,
    Scan scan,
    Social social
) {
    public record Jwt(
        String secret,
        long accessTokenHours,
        long refreshTokenDays
    ) {}

    public record Scan(
        int executorPoolSize,
        int taskTimeoutSeconds
    ) {}

    public record Social(
        OAuthClient google,
        OAuthClient kakao,
        OAuthClient github
    ) {}

    public record OAuthClient(
        String clientId,
        String clientSecret
    ) {}
}
