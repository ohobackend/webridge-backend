package com.webridge.scan.dto;

public record ScanCreateResponse(
        ScanDetailDto scan,
        String taskId,
        String message
) {}
