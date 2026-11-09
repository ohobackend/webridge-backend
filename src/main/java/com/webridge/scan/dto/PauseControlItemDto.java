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
        String tag
) {
    public static PauseControlItemDto from(PauseControlResult r) {
        return new PauseControlItemDto(
                r.getId(),
                r.getElementHtml(),
                r.isCompliant(),
                r.getMessage(),
                r.getCreatedAt(),
                r.isHasAutoplay(),
                r.isHasPause(),
                r.getTag()
        );
    }
}
