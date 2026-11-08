package com.webridge.auth.dto;

public record VerificationLinkResult(
    boolean success,
    String message,
    String email
) {}
