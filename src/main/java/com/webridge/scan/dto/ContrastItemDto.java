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
        OffsetDateTime createdAt
) {
    public static ContrastItemDto from(ContrastResult r) {
        return new ContrastItemDto(
                r.getId(),
                r.getTextContent(),
                r.getElementHtml(),
                r.getForegroundColor(),
                r.getBackgroundColor(),
                r.getContrastRatio(),
                r.isWcagCompliant(),
                r.getCreatedAt()
        );
    }
}
