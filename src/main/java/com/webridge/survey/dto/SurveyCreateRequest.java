package com.webridge.survey.dto;

import java.util.List;

public record SurveyCreateRequest(
    String email,
    Boolean privacyAgreed,
    List<String> companyType,
    String companyTypeOther,
    List<String> usageReason,
    String usageReasonOther,
    Integer satisfactionOverall,
    Integer satisfactionAccuracy,
    Integer satisfactionReuse,
    Integer satisfactionRecommend,
    String userType,
    String userTypeOther,
    String purchaseMethod,
    String purchaseMethodOther,
    List<String> usageMethod,
    String usageMethodOther,
    List<String> futureFeature,
    String futureFeatureOther,
    String improvementOpinion
) {}
