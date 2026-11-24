package com.webridge.scan.dto;

import com.webridge.scan.entity.ContrastResult;

import java.time.OffsetDateTime;

public record ContrastItemDto(
        Long id,
        String textContent,
        String elementHtml,
        String foregroundColor,
        String backgroundColor,
        double contrastRatio,
        boolean wcagCompliant,
        OffsetDateTime createdAt,
        String screenshotUrl,
        String captureUrl
) {
    public static ContrastItemDto from(ContrastResult r) {
        String url = r.getScreenshotUrl() != null && !r.getScreenshotUrl().isBlank()
                ? r.getScreenshotUrl()
                : (r.getCaptureUrl() != null && !r.getCaptureUrl().isBlank() ? r.getCaptureUrl() : extractImageUrl(r.getElementHtml()));
        return new ContrastItemDto(
                r.getId(),
                r.getTextContent(),
                r.getElementHtml(),
                r.getForegroundColor(),
                r.getBackgroundColor(),
                r.getContrastRatio(),
                r.isWcagCompliant(),
                r.getCreatedAt(),
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
