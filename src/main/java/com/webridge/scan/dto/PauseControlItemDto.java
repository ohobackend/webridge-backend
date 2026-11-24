package com.webridge.scan.dto;

import com.webridge.scan.entity.PauseControlResult;

import java.time.OffsetDateTime;

public record PauseControlItemDto(
        Long id,
        String elementHtml,
        boolean compliant,
        String message,
        OffsetDateTime createdAt,
        boolean hasAutoplay,
        boolean hasPause,
        String tag,
        String screenshotUrl,
        String captureUrl
) {
    public static PauseControlItemDto from(PauseControlResult r) {
        String url = r.getScreenshotUrl() != null && !r.getScreenshotUrl().isBlank()
                ? r.getScreenshotUrl()
                : (r.getCaptureUrl() != null && !r.getCaptureUrl().isBlank() ? r.getCaptureUrl() : extractImageUrl(r.getElementHtml()));
        return new PauseControlItemDto(
                r.getId(),
                r.getElementHtml(),
                r.isCompliant(),
                r.getMessage(),
                r.getCreatedAt(),
                r.isHasAutoplay(),
                r.isHasPause(),
                r.getTag(),
                url,
                url
        );
    }

    private static String extractImageUrl(String html) {
        if (html == null || html.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:src|poster|data-src)\\s*=\\s*['\"]([^'\"]+)['\"]", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
        if (m.find()) {
            return m.group(1);
        }
        java.util.regex.Matcher bgMatcher = java.util.regex.Pattern.compile("url\\(['\"]?([^'\")]+)['\"]?\\)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
        if (bgMatcher.find()) {
            return bgMatcher.group(1);
        }
        return null;
    }
}
