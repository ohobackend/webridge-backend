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
        OffsetDateTime createdAt
) {
    public static TableItemDto from(TableResult r) {
        return new TableItemDto(
                r.getId(),
                r.getHeadersCount(),
                r.getRowsCount(),
                r.getElementHtml(),
                r.isCompliant(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
