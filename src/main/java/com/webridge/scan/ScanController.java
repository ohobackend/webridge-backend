package com.webridge.scan;

import com.webridge.auth.dto.AuthUser;
import com.webridge.common.dto.PaginatedResponse;
import com.webridge.scan.dto.ScanCreateRequest;
import com.webridge.scan.dto.ScanCreateResponse;
import com.webridge.scan.dto.ScanDetailDto;
import com.webridge.scan.dto.ScanListItemDto;
import com.webridge.scan.dto.ScanProgressDto;
import com.webridge.scan.dto.ScanRestartResponse;
import com.webridge.scan.progress.ScanSseService;
import com.webridge.scan.service.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
@Tag(name = "Scan", description = "웹 접근성 검사 API")
public class ScanController {

    private final ScanService scanService;
    private final ScanSseService sseService;

    @PostMapping({"/create", "/create/"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "웹 접근성 검사 시작", description = "새로운 웹 접근성 검사를 시작합니다.")
    public ScanCreateResponse create(
            @AuthenticationPrincipal AuthUser me,
            @Valid @RequestBody ScanCreateRequest body
    ) {
        return scanService.create(me.getId(), body.url());
    }

    @GetMapping({"", "/"})
    @Operation(summary = "웹 접근성 검사 목록 조회", description = "사용자의 웹 접근성 검사 목록을 페이지네이션과 함께 조회합니다.")
    public PaginatedResponse<ScanListItemDto> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "-created_at") String ordering,
            HttpServletRequest req
    ) {
        return scanService.list(me.getId(), status, ordering, page, pageSize, req);
    }

    @GetMapping({"/{scanId}", "/{scanId}/"})
    @Operation(summary = "웹 접근성 검사 상세 조회", description = "특정 웹 접근성 검사의 상세 정보와 모든 검사 결과를 조회합니다.")
    public ScanDetailDto detail(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable UUID scanId
    ) {
        return scanService.detail(me.getId(), scanId);
    }

    @DeleteMapping({"/{scanId}/delete", "/{scanId}/delete/"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "웹 접근성 검사 삭제", description = "웹 접근성 검사를 삭제합니다. 진행 중인 검사는 삭제할 수 없습니다.")
    public void delete(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable UUID scanId
    ) {
        scanService.delete(me.getId(), scanId);
    }

    @GetMapping({"/{scanId}/progress", "/{scanId}/progress/"})
    @Operation(summary = "웹 접근성 검사 진행 상황 조회", description = "실시간으로 검사 진행 상황을 확인합니다.")
    public ScanProgressDto progress(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable UUID scanId
    ) {
        return scanService.progress(me.getId(), scanId);
    }

    @PostMapping({"/{scanId}/restart", "/{scanId}/restart/"})
    @Operation(summary = "웹 접근성 검사 재시작", description = "검사를 다시 시작합니다. 기존 결과는 모두 삭제됩니다.")
    public ScanRestartResponse restart(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable UUID scanId
    ) {
        return scanService.restart(me.getId(), scanId);
    }

    @GetMapping(value = {"/{scanId}/stream", "/{scanId}/stream/"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "웹 접근성 검사 실시간 스트리밍 (SSE)", description = "Server-Sent Events를 통해 실시간으로 검사 진행 상황을 스트리밍합니다.")
    public SseEmitter stream(
            @PathVariable UUID scanId,
            @AuthenticationPrincipal AuthUser me,
            HttpServletResponse res
    ) {
        res.setHeader("Cache-Control", "no-cache");
        res.setHeader("X-Accel-Buffering", "no");
        return sseService.subscribe(scanId, me.getId());
    }
}
