package com.webridge.scan.service;

import com.webridge.alttextfeedback.entity.AltTextFeedback;
import com.webridge.alttextfeedback.repository.AltTextFeedbackRepository;
import com.webridge.common.dto.PaginatedResponse;
import com.webridge.common.exception.ApiException;
import com.webridge.config.WebridgeProperties;
import com.webridge.scan.dto.ScanCreateResponse;
import com.webridge.scan.dto.ScanDetailDto;
import com.webridge.scan.dto.ScanListItemDto;
import com.webridge.scan.dto.ScanProgressDto;
import com.webridge.scan.dto.ScanRestartResponse;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.progress.ScanSseService;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.scan.repository.AltTextResultRepository;
import com.webridge.scan.repository.BasicLanguageResultRepository;
import com.webridge.scan.repository.ContrastResultRepository;
import com.webridge.scan.repository.FlashingResultRepository;
import com.webridge.scan.repository.HeadingResultRepository;
import com.webridge.scan.repository.KeyboardResultRepository;
import com.webridge.scan.repository.LabelResultRepository;
import com.webridge.scan.repository.MarkupErrorResultRepository;
import com.webridge.scan.repository.PauseControlResultRepository;
import com.webridge.scan.repository.ResponseTimeResultRepository;
import com.webridge.scan.repository.TableResultRepository;
import com.webridge.scan.repository.VideoAutoplayResultRepository;
import com.webridge.scan.repository.VideoCaptionResultRepository;
import com.webridge.scan.runner.ScanTaskRunner;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanService {

    private final AccessibilityScanRepository scanRepo;
    private final UserRepository userRepo;
    private final AltTextFeedbackRepository feedbackRepo;
    private final ScanTaskRunner taskRunner;
    private final WebridgeProperties props;

    private final AltTextResultRepository altTextRepo;
    private final ContrastResultRepository contrastRepo;
    private final KeyboardResultRepository keyboardRepo;
    private final LabelResultRepository labelRepo;
    private final TableResultRepository tableRepo;
    private final VideoCaptionResultRepository videoCaptionRepo;
    private final VideoAutoplayResultRepository videoAutoplayRepo;
    private final BasicLanguageResultRepository basicLanguageRepo;
    private final MarkupErrorResultRepository markupErrorRepo;
    private final HeadingResultRepository headingRepo;
    private final ResponseTimeResultRepository responseTimeRepo;
    private final PauseControlResultRepository pauseControlRepo;
    private final FlashingResultRepository flashingRepo;

    @Transactional
    public ScanCreateResponse create(Long userId, String url) {
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    Map.of("url", List.of("올바른 URL 형식이 아닙니다. http:// 또는 https://로 시작해야 합니다.")));
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));

        AccessibilityScan scan = AccessibilityScan.builder()
                .user(user)
                .url(url)
                .status(ScanStatus.PENDING)
                .progress(0)
                .build();
        scanRepo.save(scan);

        UUID taskId = UUID.randomUUID();
        taskRunner.run(scan.getId());

        ScanDetailDto detail = ScanDetailDto.from(scan, Map.of(), getBaseUrl());
        return new ScanCreateResponse(detail, taskId.toString(), "웹 접근성 검사가 시작되었습니다.");
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ScanListItemDto> list(Long userId, String statusFilter, String ordering, int page, int pageSize, HttpServletRequest req) {
        int pageNumber = Math.max(1, page);
        int size = Math.min(Math.max(1, pageSize), 100);

        Sort sort = resolveSort(ordering);
        PageRequest pageRequest = PageRequest.of(pageNumber - 1, size, sort);

        Page<AccessibilityScan> paged;
        if (statusFilter != null && !statusFilter.isBlank()) {
            ScanStatus status = ScanStatus.fromValue(statusFilter);
            paged = scanRepo.findByUserIdAndStatus(userId, status, pageRequest);
        } else {
            paged = scanRepo.findByUserId(userId, pageRequest);
        }

        List<ScanListItemDto> items = paged.getContent().stream()
                .map(ScanListItemDto::from)
                .toList();

        String baseUrl = req.getRequestURL().toString();
        String next = paged.hasNext() ? buildPageUrl(baseUrl, statusFilter, ordering, pageNumber + 1, size) : null;
        String prev = paged.hasPrevious() ? buildPageUrl(baseUrl, statusFilter, ordering, pageNumber - 1, size) : null;

        return new PaginatedResponse<>(paged.getTotalElements(), next, prev, items);
    }

    @Transactional(readOnly = true)
    public ScanDetailDto detail(Long userId, UUID scanId) {
        AccessibilityScan scan = scanRepo.findById(scanId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "찾을 수 없습니다.")));

        if (!scan.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "찾을 수 없습니다."));
        }

        Map<Long, String> feedbackMap = new HashMap<>();
        List<AltTextFeedback> feedbacks = feedbackRepo.findByUserId(userId);
        for (AltTextFeedback fb : feedbacks) {
            if (fb.getAltTextResult() != null) {
                feedbackMap.put(fb.getAltTextResult().getId(), fb.getRating());
            }
        }

        return ScanDetailDto.from(scan, feedbackMap, getBaseUrl());
    }

    @Transactional
    public void delete(Long userId, UUID scanId) {
        AccessibilityScan scan = scanRepo.findById(scanId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "찾을 수 없습니다.")));

        if (!scan.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "찾을 수 없습니다."));
        }

        if (scan.getStatus() == ScanStatus.PROCESSING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "진행 중인 검사는 삭제할 수 없습니다."));
        }

        scanRepo.delete(scan);
    }

    @Transactional(readOnly = true)
    public ScanProgressDto progress(Long userId, UUID scanId) {
        AccessibilityScan scan = scanRepo.findById(scanId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "찾을 수 없습니다.")));

        if (!scan.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "찾을 수 없습니다."));
        }

        return new ScanProgressDto(
                scan.getId(),
                scan.getStatus().getValue(),
                scan.getProgress(),
                ScanSseService.getCurrentTestName(scan),
                ScanSseService.getProgressMessage(scan)
        );
    }

    @Transactional
    public ScanRestartResponse restart(Long userId, UUID scanId) {
        AccessibilityScan scan = scanRepo.findById(scanId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "찾을 수 없습니다.")));

        if (!scan.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "찾을 수 없습니다."));
        }

        if (scan.getStatus() == ScanStatus.PROCESSING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("error", "이미 진행 중인 검사입니다."));
        }

        // 1. Delete prior results
        altTextRepo.deleteByScanId(scanId);
        contrastRepo.deleteByScanId(scanId);
        keyboardRepo.deleteByScanId(scanId);
        labelRepo.deleteByScanId(scanId);
        tableRepo.deleteByScanId(scanId);
        videoCaptionRepo.deleteByScanId(scanId);
        videoAutoplayRepo.deleteByScanId(scanId);
        basicLanguageRepo.deleteByScanId(scanId);
        markupErrorRepo.deleteByScanId(scanId);
        headingRepo.deleteByScanId(scanId);
        responseTimeRepo.deleteByScanId(scanId);
        pauseControlRepo.deleteByScanId(scanId);
        flashingRepo.deleteByScanId(scanId);

        // 2. Reset scan
        scan.setStatus(ScanStatus.PENDING);
        scan.setProgress(0);
        scan.setTotalIssues(0);
        scan.setComplianceScore(0.0);
        scan.setAltTextCompleted(false);
        scan.setContrastCompleted(false);
        scan.setKeyboardCompleted(false);
        scan.setLabelCompleted(false);
        scan.setTableCompleted(false);
        scan.setVideoCaptionCompleted(false);
        scan.setVideoAutoplayCompleted(false);
        scan.setBasicLanguageCompleted(false);
        scan.setMarkupErrorCompleted(false);
        scan.setHeadingCompleted(false);
        scan.setResponseTimeCompleted(false);
        scan.setPauseControlCompleted(false);
        scan.setFlashingCompleted(false);
        scan.setCompletedAt(null);
        scan.setErrorMessage(null);
        scan.setUpdatedAt(OffsetDateTime.now());
        scanRepo.save(scan);

        UUID taskId = UUID.randomUUID();
        taskRunner.run(scan.getId());

        ScanDetailDto detail = ScanDetailDto.from(scan, Map.of(), getBaseUrl());
        return new ScanRestartResponse(detail, taskId.toString(), "검사가 다시 시작되었습니다.");
    }

    private Sort resolveSort(String ordering) {
        if ("created_at".equals(ordering)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("updated_at".equals(ordering)) {
            return Sort.by(Sort.Direction.ASC, "updatedAt");
        } else if ("-updated_at".equals(ordering)) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private String buildPageUrl(String baseUrl, String statusFilter, String ordering, int page, int pageSize) {
        StringBuilder sb = new StringBuilder(baseUrl).append("?page=").append(page).append("&page_size=").append(pageSize);
        if (statusFilter != null && !statusFilter.isBlank()) {
            sb.append("&status=").append(statusFilter);
        }
        if (ordering != null && !ordering.isBlank()) {
            sb.append("&ordering=").append(ordering);
        }
        return sb.toString();
    }

    private String getBaseUrl() {
        return (props.backendUrl() != null && !props.backendUrl().isBlank())
                ? props.backendUrl()
                : "http://127.0.0.1:8080";
    }
}
