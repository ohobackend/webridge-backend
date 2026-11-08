package com.webridge.survey;

import com.webridge.auth.dto.AuthUser;
import com.webridge.survey.dto.SurveyCreateRequest;
import com.webridge.survey.dto.SurveyDto;
import com.webridge.survey.dto.SurveyTriggerDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/survey")
@RequiredArgsConstructor
@Tag(name = "Survey", description = "만족도 설문조사 및 팝업 트리거 API")
public class SurveyController {

    private final SurveyService surveyService;

    @PostMapping({"/create", "/create/"})
    @Operation(summary = "만족도 설문조사 제출", description = "사용자의 만족도 설문조사를 제출합니다.")
    public ResponseEntity<SurveyDto> createSurvey(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestBody SurveyCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(surveyService.createSurvey(authUser.getId(), request));
    }

    @GetMapping({"/check-trigger", "/check-trigger/"})
    @Operation(summary = "설문조사 팝업 표시 여부 확인", description = "사용자에게 설문조사 팝업을 표시할지 확인합니다.")
    public ResponseEntity<SurveyTriggerDto> checkSurveyTrigger(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(surveyService.checkTrigger(authUser.getId()));
    }

    @PostMapping({"/mark-shown", "/mark-shown/"})
    @Operation(summary = "설문조사 팝업 다시 표시 안 함", description = "설문조사 팝업이 표시되었음을 기록합니다.")
    public ResponseEntity<Map<String, String>> markSurveyShown(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(surveyService.markShown(authUser.getId()));
    }
}
