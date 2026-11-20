package com.webridge.admin.dto;

import com.webridge.terms.entity.Terms;

import java.time.OffsetDateTime;

public record AdminTermsDto(
    Long id,
    String type,
    String typeDisplay,
    String version,
    String changeType,
    String changeTypeDisplay,
    boolean requiresReagreement,
    String changeSummary,
    String content,
    boolean isActive,
    OffsetDateTime createdAt
) {
    public static AdminTermsDto from(Terms terms) {
        String typeDisplay = switch (terms.getType() != null ? terms.getType().toLowerCase() : "") {
            case "service" -> "서비스 이용약관";
            case "privacy" -> "개인정보 수집 이용 동의";
            case "marketing" -> "마케팅 목적 개인정보 수집 이용 동의";
            default -> terms.getType();
        };

        String changeTypeDisplay = switch (terms.getChangeType() != null ? terms.getChangeType().toLowerCase() : "") {
            case "major" -> "중대 변경 (재동의 필수)";
            case "minor" -> "일반 변경 (재동의 권장)";
            case "patch" -> "경미 변경 (재동의 불필요)";
            default -> terms.getChangeType();
        };

        return new AdminTermsDto(
            terms.getId(),
            terms.getType(),
            typeDisplay,
            terms.getVersion(),
            terms.getChangeType(),
            changeTypeDisplay,
            terms.isRequiresReagreement(),
            terms.getChangeSummary(),
            terms.getContent(),
            terms.isActive(),
            terms.getCreatedAt()
        );
    }
}
