package com.webridge.user.dto;

public record UserExtraInfoRequest(
    String pathToWebridge,
    String pathToWebridgeOther,
    String occupation,
    String occupationOther
) {}
