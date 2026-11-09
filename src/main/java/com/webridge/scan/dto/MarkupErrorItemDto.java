package com.webridge.scan.dto;

import com.webridge.scan.entity.MarkupErrorResult;

import java.time.OffsetDateTime;

public record MarkupErrorItemDto(
        Long id,
        String errorType,
        String errorDetail,
        String elementId,
        String elementHtml,
        boolean compliant,
        String message,
        OffsetDateTime createdAt
) {
    public static MarkupErrorItemDto from(MarkupErrorResult r) {
        return new MarkupErrorItemDto(
                r.getId(),
                r.getErrorType(),
                r.getErrorDetail(),
                r.getElementId(),
                r.getElementHtml(),
                r.isCompliant(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
