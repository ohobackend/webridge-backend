package com.webridge.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Schema(description = "Ping API 응답")
public record PingResponse(
    @Schema(description = "응답 메시지", example = "pong")
    String message,

    @Schema(description = "응답 시간 (ISO 형식)", example = "2026-11-05T19:00:00.000000")
    String timestamp,

    @Schema(description = "서버 이름", example = "WEBridge-api")
    String server,

    @Schema(description = "서버 버전", example = "1.0.0")
    String version
) {
    public static PingResponse ok() {
        return new PingResponse(
            "pong",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "WEBridge-api",
            "1.0.0"
        );
    }
}
