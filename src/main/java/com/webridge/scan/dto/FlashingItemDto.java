package com.webridge.scan.dto;

import com.webridge.scan.entity.FlashingResult;

import java.time.OffsetDateTime;

public record FlashingItemDto(
        Long id,
        String flashingType,
        String elementHtml,
        Integer duration,
        boolean compliant,
        String message,
        OffsetDateTime createdAt
) {
    public static FlashingItemDto from(FlashingResult r) {
        return new FlashingItemDto(
                r.getId(),
                r.getFlashingType(),
                r.getElementHtml(),
                r.getDuration(),
                r.isCompliant(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
