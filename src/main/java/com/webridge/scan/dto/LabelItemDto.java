package com.webridge.scan.dto;

import com.webridge.scan.entity.LabelResult;

import java.time.OffsetDateTime;

public record LabelItemDto(
        Long id,
        String inputType,
        String elementHtml,
        boolean labelPresent,
        String message,
        OffsetDateTime createdAt
) {
    public static LabelItemDto from(LabelResult r) {
        return new LabelItemDto(
                r.getId(),
                r.getInputType(),
                r.getElementHtml(),
                r.isLabelPresent(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
