package com.webridge.survey.dto;

import com.webridge.survey.entity.SurveyTrigger;

public record SurveyTriggerDto(
    int completedScansCount,
    boolean hasIssuesFound,
    boolean surveyShown,
    boolean surveyCompleted,
    boolean shouldShowSurvey
) {
    public static SurveyTriggerDto from(SurveyTrigger trigger) {
        return new SurveyTriggerDto(
            trigger.getCompletedScansCount(),
            trigger.isHasIssuesFound(),
            trigger.isSurveyShown(),
            trigger.isSurveyCompleted(),
            trigger.shouldShowSurvey()
        );
    }
}
