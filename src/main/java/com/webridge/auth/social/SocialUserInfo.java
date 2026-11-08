package com.webridge.auth.social;

public record SocialUserInfo(
    String id,
    String email,
    String name,
    String profileImage
) {}
