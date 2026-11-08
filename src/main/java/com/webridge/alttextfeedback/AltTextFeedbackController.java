package com.webridge.alttextfeedback;

import com.webridge.alttextfeedback.dto.AltTextFeedbackCreateRequest;
import com.webridge.alttextfeedback.dto.AltTextFeedbackDto;
import com.webridge.alttextfeedback.dto.AltTextFeedbackListDto;
import com.webridge.alttextfeedback.dto.AltTextResultWithFeedbackDto;
import com.webridge.auth.dto.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Alt Text Feedback", description = "AI 대체텍스트 평가 및 결과 조회 API")
public class AltTextFeedbackController {

    private final AltTextFeedbackService altTextFeedbackService;

    @PostMapping({"/api/v1/alt-text-feedback/create", "/api/v1/alt-text-feedback/create/"})
    @Operation(summary = "AI 대체텍스트 평가 생성", description = "AI가 제안한 대체텍스트에 대해 좋아요/싫어요 평가를 남깁니다.")
    public ResponseEntity<AltTextFeedbackDto> createFeedback(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestBody AltTextFeedbackCreateRequest request
    ) {
        AltTextFeedbackService.FeedbackCreationResult result = altTextFeedbackService.createOrUpdateFeedback(authUser.getId(), request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.dto());
    }

    @GetMapping({"/api/v1/alt-text-feedback/my", "/api/v1/alt-text-feedback/my/"})
    @Operation(summary = "내 AI 대체텍스트 평가 목록 조회", description = "현재 로그인한 사용자가 남긴 모든 AI 대체텍스트 평가를 조회합니다.")
    public ResponseEntity<List<AltTextFeedbackListDto>> getUserFeedbacks(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(altTextFeedbackService.getUserFeedbacks(authUser.getId()));
    }

    @GetMapping({"/api/v1/alt-text-feedback/result/{altTextResultId}", "/api/v1/alt-text-feedback/result/{altTextResultId}/"})
    @Operation(summary = "특정 대체텍스트 결과의 사용자 평가 조회", description = "특정 대체텍스트 결과에 대한 현재 사용자의 평가를 조회합니다.")
    public ResponseEntity<AltTextFeedbackDto> getFeedbackByResult(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable Long altTextResultId
    ) {
        return ResponseEntity.ok(altTextFeedbackService.getFeedbackByResultId(authUser.getId(), altTextResultId));
    }

    @DeleteMapping({"/api/v1/alt-text-feedback/{feedbackId}/delete", "/api/v1/alt-text-feedback/{feedbackId}/delete/", "/api/v1/alt-text-feedback/{feedbackId}", "/api/v1/alt-text-feedback/{feedbackId}/"})
    @Operation(summary = "AI 대체텍스트 평가 삭제", description = "특정 대체텍스트 결과에 대한 사용자의 평가를 삭제합니다.")
    public ResponseEntity<Void> deleteFeedback(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable Long feedbackId
    ) {
        altTextFeedbackService.deleteFeedback(authUser.getId(), feedbackId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/api/v1/scans/{scanId}/alt-text-results", "/api/v1/scans/{scanId}/alt-text-results/"})
    @Operation(summary = "특정 스캔의 대체텍스트 결과와 사용자 평가 조회", description = "특정 스캔의 모든 대체텍스트 검사 결과와 현재 사용자의 평가 상태를 함께 조회합니다.")
    public ResponseEntity<List<AltTextResultWithFeedbackDto>> getScanAltTextResultsWithFeedback(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable UUID scanId
    ) {
        return ResponseEntity.ok(altTextFeedbackService.getScanAltTextResultsWithFeedback(authUser.getId(), scanId));
    }
}
