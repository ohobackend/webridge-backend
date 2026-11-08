package com.webridge.survey.dto;

import com.webridge.survey.entity.Survey;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SurveyDto(
    UUID id,
    String email,
    boolean privacyAgreed,
    List<String> companyType,
    String companyTypeOther,
    List<String> usageReason,
    String usageReasonOther,
    int satisfactionOverall,
    int satisfactionAccuracy,
    int satisfactionReuse,
    int satisfactionRecommend,
    String userType,
    String userTypeOther,
    String purchaseMethod,
    String purchaseMethodOther,
    List<String> usageMethod,
    String usageMethodOther,
    List<String> futureFeature,
    String futureFeatureOther,
    String improvementOpinion,
    OffsetDateTime createdAt
) {
    public static SurveyDto from(Survey survey) {
        return new SurveyDto(
            survey.getId(),
            survey.getEmail(),
            survey.isPrivacyAgreed(),
            survey.getCompanyType(),
            survey.getCompanyTypeOther(),
            survey.getUsageReason(),
            survey.getUsageReasonOther(),
            survey.getSatisfactionOverall(),
            survey.getSatisfactionAccuracy(),
            survey.getSatisfactionReuse(),
            survey.getSatisfactionRecommend(),
            survey.getUserType(),
            survey.getUserTypeOther(),
            survey.getPurchaseMethod(),
            survey.getPurchaseMethodOther(),
            survey.getUsageMethod(),
            survey.getUsageMethodOther(),
            survey.getFutureFeature(),
            survey.getFutureFeatureOther(),
            survey.getImprovementOpinion(),
            survey.getCreatedAt()
        );
    }
}
