package com.webridge.terms.dto;

import java.util.Map;

public record TermsUpdateResponse(
    String message,
    Map<String, Boolean> userAgreements
) {}
