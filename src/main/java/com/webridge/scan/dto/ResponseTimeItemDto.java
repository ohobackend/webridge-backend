package com.webridge.scan.dto;

import com.webridge.scan.entity.ResponseTimeResult;

import java.time.OffsetDateTime;

public record ResponseTimeItemDto(
        Long id,
        boolean hasTimeLimit,
        int shortTimeouts,
        String elementHtml,
        boolean compliant,
        String message,
        OffsetDateTime createdAt
) {
    public static ResponseTimeItemDto from(ResponseTimeResult r) {
        return new ResponseTimeItemDto(
                r.getId(),
                r.isHasTimeLimit(),
                r.getShortTimeouts(),
                r.getElementHtml(),
                r.isCompliant(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
