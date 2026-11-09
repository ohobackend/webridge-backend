package com.webridge.scan.dto;

import com.webridge.scan.entity.BasicLanguageResult;

import java.time.OffsetDateTime;

public record BasicLanguageItemDto(
        Long id,
        String langAttribute,
        boolean compliant,
        String message,
        OffsetDateTime createdAt
) {
    public static BasicLanguageItemDto from(BasicLanguageResult r) {
        return new BasicLanguageItemDto(
                r.getId(),
                r.getLangAttribute(),
                r.isCompliant(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
