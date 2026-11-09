package com.webridge.scan.dto;

import java.util.UUID;

public record ScanProgressDto(
        UUID scanId,
        String status,
        int progress,
        String currentTest,
        String message
) {}
