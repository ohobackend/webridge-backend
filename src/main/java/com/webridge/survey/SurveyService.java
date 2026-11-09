package com.webridge.survey;

import com.webridge.common.exception.ApiException;
import com.webridge.survey.dto.SurveyCreateRequest;
import com.webridge.survey.dto.SurveyDto;
import com.webridge.survey.dto.SurveyTriggerDto;
import com.webridge.survey.entity.Survey;
import com.webridge.survey.entity.SurveyTrigger;
import com.webridge.survey.repository.SurveyRepository;
import com.webridge.survey.repository.SurveyTriggerRepository;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyTriggerRepository surveyTriggerRepository;
    private final UserRepository userRepository;
    private final com.webridge.scan.repository.AccessibilityScanRepository scanRepository;

    private static final Set<String> VALID_COMPANY_TYPES = Set.of("a", "b", "c", "d", "e", "f");
    private static final Set<String> VALID_USAGE_REASONS = Set.of("a", "b", "c", "d", "e");
    private static final Set<String> VALID_USAGE_METHODS = Set.of("a", "b", "c", "d");
    private static final Set<String> VALID_FUTURE_FEATURES = Set.of("a", "b", "c", "d");

    @Transactional
    public SurveyDto createSurvey(Long userId, SurveyCreateRequest req) {
        User user = getUserOrThrow(userId);
        Map<String, List<String>> errors = new LinkedHashMap<>();

        if (!Boolean.TRUE.equals(req.privacyAgreed())) {
            errors.put("privacy_agreed", List.of("개인정보 수집 이용에 동의해야 합니다."));
        }

        validateSatisfaction(errors, "satisfaction_overall", req.satisfactionOverall());
        validateSatisfaction(errors, "satisfaction_accuracy", req.satisfactionAccuracy());
        validateSatisfaction(errors, "satisfaction_reuse", req.satisfactionReuse());
        validateSatisfaction(errors, "satisfaction_recommend", req.satisfactionRecommend());

        validateChoiceList(errors, "company_type", req.companyType(), "최소 하나의 회사 유형을 선택해야 합니다.", VALID_COMPANY_TYPES);
        validateChoiceList(errors, "usage_reason", req.usageReason(), "최소 하나의 사용 이유를 선택해야 합니다.", VALID_USAGE_REASONS);
        validateChoiceList(errors, "usage_method", req.usageMethod(), "최소 하나의 이용 방식을 선택해야 합니다.", VALID_USAGE_METHODS);
        validateChoiceList(errors, "future_feature", req.futureFeature(), "최소 하나의 추후 도입 기능을 선택해야 합니다.", VALID_FUTURE_FEATURES);

        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, errors);
        }

        Survey survey = Survey.builder()
            .user(user)
            .email(req.email() != null ? req.email() : user.getEmail())
            .privacyAgreed(true)
            .companyType(req.companyType() != null ? req.companyType() : new ArrayList<>())
            .companyTypeOther(req.companyTypeOther() != null ? req.companyTypeOther() : "")
            .usageReason(req.usageReason() != null ? req.usageReason() : new ArrayList<>())
            .usageReasonOther(req.usageReasonOther() != null ? req.usageReasonOther() : "")
            .satisfactionOverall(req.satisfactionOverall())
            .satisfactionAccuracy(req.satisfactionAccuracy())
            .satisfactionReuse(req.satisfactionReuse())
            .satisfactionRecommend(req.satisfactionRecommend())
            .userType(req.userType() != null ? req.userType() : "")
            .userTypeOther(req.userTypeOther() != null ? req.userTypeOther() : "")
            .purchaseMethod(req.purchaseMethod() != null ? req.purchaseMethod() : "")
            .purchaseMethodOther(req.purchaseMethodOther() != null ? req.purchaseMethodOther() : "")
            .usageMethod(req.usageMethod() != null ? req.usageMethod() : new ArrayList<>())
            .usageMethodOther(req.usageMethodOther() != null ? req.usageMethodOther() : "")
            .futureFeature(req.futureFeature() != null ? req.futureFeature() : new ArrayList<>())
            .futureFeatureOther(req.futureFeatureOther() != null ? req.futureFeatureOther() : "")
            .improvementOpinion(req.improvementOpinion() != null ? req.improvementOpinion() : "")
            .build();

        surveyRepository.save(survey);

        SurveyTrigger trigger = surveyTriggerRepository.findByUserId(userId)
            .orElseGet(() -> SurveyTrigger.builder().user(user).build());
        trigger.setSurveyCompleted(true);
        surveyTriggerRepository.save(trigger);

        return SurveyDto.from(survey);
    }

    @Transactional
    public SurveyTriggerDto checkTrigger(Long userId) {
        User user = getUserOrThrow(userId);
        SurveyTrigger trigger = surveyTriggerRepository.findByUserId(userId)
            .orElseGet(() -> surveyTriggerRepository.save(SurveyTrigger.builder().user(user).build()));
        return SurveyTriggerDto.from(trigger);
    }

    @Transactional
    public Map<String, String> markShown(Long userId) {
        User user = getUserOrThrow(userId);
        SurveyTrigger trigger = surveyTriggerRepository.findByUserId(userId)
            .orElseGet(() -> SurveyTrigger.builder().user(user).build());
        trigger.setSurveyShown(true);
        surveyTriggerRepository.save(trigger);
        return Map.of("message", "설문조사 팝업 표시됨이 기록되었습니다.");
    }

    private void validateSatisfaction(Map<String, List<String>> errors, String fieldName, Integer value) {
        if (value == null || value < 1 || value > 5) {
            errors.put(fieldName, List.of("만족도는 1-5점 사이여야 합니다."));
        }
    }

    private void validateChoiceList(Map<String, List<String>> errors, String fieldName, List<String> list, String emptyMsg, Set<String> validChoices) {
        if (list == null || list.isEmpty()) {
            errors.put(fieldName, List.of(emptyMsg));
            return;
        }
        for (String choice : list) {
            if (!validChoices.contains(choice)) {
                errors.put(fieldName, List.of("유효하지 않은 " + fieldName + "입니다: " + choice));
                return;
            }
        }
    }

    @Transactional
    public void onScanCompleted(java.util.UUID scanId) {
        try {
            var scanOpt = scanRepository.findById(scanId);
            if (scanOpt.isEmpty()) return;
            var scan = scanOpt.get();
            var user = scan.getUser();
            if (user == null) return;

            SurveyTrigger trigger = surveyTriggerRepository.findByUserId(user.getId())
                    .orElseGet(() -> SurveyTrigger.builder().user(user).build());
            trigger.setCompletedScansCount(trigger.getCompletedScansCount() + 1);
            if (scan.getTotalIssues() > 0) {
                trigger.setHasIssuesFound(true);
            }
            surveyTriggerRepository.save(trigger);
        } catch (Exception e) {
            // Survey trigger failure should not fail scan completion
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));
    }
}
