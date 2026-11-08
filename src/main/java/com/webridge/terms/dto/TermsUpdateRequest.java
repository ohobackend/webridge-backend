package com.webridge.terms.dto;

public record TermsUpdateRequest(
    Boolean serviceTermsAgreed,
    Boolean privacyPolicyAgreed,
    Boolean marketingAgreed
) {}
