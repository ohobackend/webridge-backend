package com.webridge.feedback;

import com.webridge.auth.dto.AuthUser;
import com.webridge.common.dto.PaginatedResponse;
import com.webridge.feedback.dto.FeedbackCreateRequest;
import com.webridge.feedback.dto.FeedbackDto;
import com.webridge.feedback.dto.FeedbackUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "사용자 피드백 생성, 조회, 수정, 삭제 API")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping({"/create", "/create/"})
    @Operation(summary = "피드백 생성", description = "로그인한 사용자가 피드백을 생성합니다.")
    public ResponseEntity<FeedbackDto> createFeedback(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestBody FeedbackCreateRequest request
    ) {
        FeedbackDto response = feedbackService.createFeedback(authUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/list", "/list/"})
    @Operation(summary = "내 피드백 목록 조회", description = "로그인한 사용자의 피드백 목록을 페이지네이션으로 조회합니다.")
    public ResponseEntity<PaginatedResponse<FeedbackDto>> listFeedbacks(
        @AuthenticationPrincipal AuthUser authUser,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(feedbackService.listFeedbacks(authUser.getId(), page, pageSize, request.getRequestURI()));
    }

    @GetMapping({"/{id}", "/{id}/"})
    @Operation(summary = "내 피드백 상세 조회", description = "로그인한 사용자의 특정 피드백 상세 정보를 조회합니다.")
    public ResponseEntity<FeedbackDto> getFeedback(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(feedbackService.getFeedback(authUser.getId(), id));
    }

    @PatchMapping({"/{id}/update", "/{id}/update/", "/{id}", "/{id}/"})
    @Operation(summary = "내 피드백 수정", description = "로그인한 사용자가 본인의 피드백을 수정합니다.")
    public ResponseEntity<FeedbackDto> updateFeedbackPatch(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable Long id,
        @RequestBody FeedbackUpdateRequest request
    ) {
        return ResponseEntity.ok(feedbackService.updateFeedback(authUser.getId(), id, request));
    }

    @PutMapping({"/{id}/update", "/{id}/update/", "/{id}", "/{id}/"})
    @Operation(summary = "내 피드백 수정 (PUT)", description = "로그인한 사용자가 본인의 피드백을 수정합니다.")
    public ResponseEntity<FeedbackDto> updateFeedbackPut(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable Long id,
        @RequestBody FeedbackUpdateRequest request
    ) {
        return ResponseEntity.ok(feedbackService.updateFeedback(authUser.getId(), id, request));
    }

    @DeleteMapping({"/{id}/delete", "/{id}/delete/", "/{id}", "/{id}/"})
    @Operation(summary = "내 피드백 삭제", description = "로그인한 사용자가 본인의 피드백을 삭제합니다.")
    public ResponseEntity<Void> deleteFeedback(
        @AuthenticationPrincipal AuthUser authUser,
        @PathVariable Long id
    ) {
        feedbackService.deleteFeedback(authUser.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
