package com.webridge.terms.dto;

import com.webridge.terms.entity.Terms;

import java.time.OffsetDateTime;

public record TermsDto(
    Long id,
    String type,
    String version,
    String changeType,
    boolean requiresReagreement,
    String changeSummary,
    String content,
    boolean isActive,
    OffsetDateTime createdAt
) {
    public static TermsDto from(Terms terms) {
        return new TermsDto(
            terms.getId(),
            terms.getType(),
            terms.getVersion(),
            terms.getChangeType(),
            terms.isRequiresReagreement(),
            terms.getChangeSummary(),
            terms.getContent(),
            terms.isActive(),
            terms.getCreatedAt()
        );
    }
}
