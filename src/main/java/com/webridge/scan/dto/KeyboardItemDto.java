package com.webridge.scan.dto;

import com.webridge.scan.entity.KeyboardResult;

import java.time.OffsetDateTime;

public record KeyboardItemDto(
        Long id,
        String elementType,
        String elementHtml,
        boolean accessible,
        String message,
        OffsetDateTime createdAt
) {
    public static KeyboardItemDto from(KeyboardResult r) {
        return new KeyboardItemDto(
                r.getId(),
                r.getElementType(),
                r.getElementHtml(),
                r.isAccessible(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
