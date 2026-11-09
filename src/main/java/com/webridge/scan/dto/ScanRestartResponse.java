package com.webridge.scan.dto;

public record ScanRestartResponse(
        ScanDetailDto scan,
        String taskId,
        String message
) {}
