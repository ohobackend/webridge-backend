package com.webridge.scan.dto;

import com.webridge.scan.entity.VideoCaptionResult;

import java.time.OffsetDateTime;

public record VideoCaptionItemDto(
        Long id,
        String videoUrl,
        String elementHtml,
        boolean hasThumbnail,
        boolean hasTranscript,
        boolean hasAudioDescription,
        boolean hasAriaLabel,
        boolean compliant,
        String message,
        OffsetDateTime createdAt
) {
    public static VideoCaptionItemDto from(VideoCaptionResult r) {
        return new VideoCaptionItemDto(
                r.getId(),
                r.getVideoUrl(),
                r.getElementHtml(),
                r.isHasThumbnail(),
                r.isHasTranscript(),
                r.isHasAudioDescription(),
                r.isHasAriaLabel(),
                r.isCompliant(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
