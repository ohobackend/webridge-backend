package com.webridge.scan.dto;

import com.webridge.scan.entity.VideoAutoplayResult;

import java.time.OffsetDateTime;

public record VideoAutoplayItemDto(
        Long id,
        String videoUrl,
        String elementHtml,
        boolean autoplayDisabled,
        boolean keyboardAccessible,
        boolean compliant,
        String message,
        OffsetDateTime createdAt
) {
    public static VideoAutoplayItemDto from(VideoAutoplayResult r) {
        return new VideoAutoplayItemDto(
                r.getId(),
                r.getVideoUrl(),
                r.getElementHtml(),
                r.isAutoplayDisabled(),
                r.isKeyboardAccessible(),
                r.isCompliant(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
