package com.webridge.scan.progress;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScanProgressMessage(
        String scanId,
        String status,
        int progress,
        String currentTest,
        String message,
        String timestamp,
        String type,
        Boolean isFinal,
        String error
) {
    public static ScanProgressMessage of(UUID scanId, String status, int progress, String currentTest, String message, String type, Boolean isFinal) {
        return new ScanProgressMessage(
                scanId.toString(),
                status,
                progress,
                currentTest,
                message,
                OffsetDateTime.now().toString(),
                type,
                isFinal,
                null
        );
    }

    public static ScanProgressMessage error(UUID scanId, String error) {
        return new ScanProgressMessage(
                scanId.toString(),
                "failed",
                -1,
                null,
                "검사 중 오류가 발생했습니다.",
                OffsetDateTime.now().toString(),
                "error",
                true,
                error
        );
    }

    public Map<String, Object> toMap() {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("scan_id", scanId);
        if (status != null) map.put("status", status);
        map.put("progress", progress);
        if (currentTest != null) map.put("current_test", currentTest);
        if (message != null) map.put("message", message);
        if (timestamp != null) map.put("timestamp", timestamp);
        if (type != null) map.put("type", type);
        if (isFinal != null) map.put("final", isFinal);
        if (error != null) map.put("error", error);
        return map;
    }
}
