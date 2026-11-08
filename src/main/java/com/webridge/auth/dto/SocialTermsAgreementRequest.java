package com.webridge.auth.dto;

public record SocialTermsAgreementRequest(
    Boolean serviceTermsAgreed,
    Boolean ageVerificationAgreed,
    Boolean privacyPolicyAgreed,
    Boolean marketingAgreed
) {}
