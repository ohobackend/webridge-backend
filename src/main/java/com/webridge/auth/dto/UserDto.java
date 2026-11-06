package com.webridge.auth.dto;

import com.webridge.user.entity.User;

import java.time.OffsetDateTime;
import java.util.Map;

public record UserDto(
    Long id,
    String email,
    String name,
    String provider,
    OffsetDateTime createdAt,
    AgreementsDto agreements,
    boolean needsTermsUpdate
) {
    public record AgreementsDto(
        boolean serviceTerms,
        boolean ageVerification,
        boolean privacyPolicy,
        boolean marketing,
        boolean allRequiredAgreed,
        Map<String, String> versions
    ) {}

    public static UserDto of(User user, boolean needsTermsUpdate) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getProvider(),
            user.getCreatedAt(),
            new AgreementsDto(
                user.isServiceTermsAgreed(),
                user.isAgeVerificationAgreed(),
                user.isPrivacyPolicyAgreed(),
                user.isMarketingAgreed(),
                user.hasRequiredAgreements(),
                Map.of(
                    "service", user.getServiceTermsVersion() != null ? user.getServiceTermsVersion() : "1.0",
                    "privacy", user.getPrivacyPolicyVersion() != null ? user.getPrivacyPolicyVersion() : "1.0",
                    "marketing", user.getMarketingVersion() != null ? user.getMarketingVersion() : "1.0"
                )
            ),
            needsTermsUpdate
        );
    }
}
