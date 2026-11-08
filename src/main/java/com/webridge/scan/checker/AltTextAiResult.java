package com.webridge.scan.checker;

public record AltTextAiResult(
        String type,
        String grade,
        String reason,
        String improvement,
        boolean hasError,
        int compliant
) {
    public static AltTextAiResult error(String reason, String improvement, int compliant) {
        return new AltTextAiResult("알 수 없음", "매우낮음", reason, improvement, true, compliant);
    }
}
