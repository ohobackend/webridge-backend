package com.webridge.scan.dto;

import com.webridge.scan.entity.TableResult;

import java.time.OffsetDateTime;

public record TableItemDto(
        Long id,
        int headersCount,
        int rowsCount,
        String elementHtml,
        boolean compliant,
        String message,
        OffsetDateTime createdAt,
        String screenshotUrl,
        String captureUrl
) {
    public static TableItemDto from(TableResult r) {
        String url = r.getScreenshotUrl() != null && !r.getScreenshotUrl().isBlank()
                ? r.getScreenshotUrl()
                : (r.getCaptureUrl() != null && !r.getCaptureUrl().isBlank() ? r.getCaptureUrl() : extractImageUrl(r.getElementHtml()));
        return new TableItemDto(
                r.getId(),
                r.getHeadersCount(),
                r.getRowsCount(),
                r.getElementHtml(),
                r.isCompliant(),
                r.getMessage(),
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
