package com.webridge.scan.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.common.exception.ApiException;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.repository.AccessibilityScanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanSseService {

    private static final long SSE_TIMEOUT = 300_000L; // 5분
    private final AccessibilityScanRepository scanRepo;
    private final ObjectMapper om;

    private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID scanId, Long userId) {
        AccessibilityScan scan = scanRepo.findById(scanId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("error", "검사를 찾을 수 없습니다.")));

        if (!scan.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, Map.of("detail", "이 검사에 접근할 권한이 없습니다."));
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(scanId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(scanId, emitter));
        emitter.onTimeout(() -> {
            removeEmitter(scanId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> removeEmitter(scanId, emitter));

        // 1. Initial event
        Map<String, Object> initialData = new LinkedHashMap<>();
        initialData.put("scan_id", scan.getId().toString());
        initialData.put("status", scan.getStatus().getValue());
        initialData.put("progress", scan.getProgress());
        initialData.put("current_test", getCurrentTestName(scan));
        initialData.put("message", getProgressMessage(scan));
        initialData.put("timestamp", OffsetDateTime.now().toString());
        initialData.put("type", "initial");
        sendEvent(emitter, initialData);

        // 2. If already finished, send final event and complete
        if (scan.getStatus() == ScanStatus.COMPLETED || scan.getStatus() == ScanStatus.FAILED || scan.getStatus() == ScanStatus.CANCELLED) {
            Map<String, Object> finalData = new LinkedHashMap<>();
            finalData.put("scan_id", scan.getId().toString());
            finalData.put("status", scan.getStatus().getValue());
            finalData.put("progress", scan.getStatus() == ScanStatus.COMPLETED ? 100 : scan.getProgress());
            finalData.put("message", scan.getStatus() == ScanStatus.COMPLETED ? "검사가 완료되었습니다." : "검사 중 오류가 발생했습니다.");
            finalData.put("timestamp", OffsetDateTime.now().toString());
            finalData.put("final", true);
            sendEvent(emitter, finalData);
            emitter.complete();
            removeEmitter(scanId, emitter);
        }

        return emitter;
    }

    public void emitProgress(UUID scanId, Map<String, Object> data) {
        Set<SseEmitter> set = emitters.get(scanId);
        if (set == null || set.isEmpty()) return;

        boolean isFinal = Boolean.TRUE.equals(data.get("final"));

        for (SseEmitter emitter : set) {
            try {
                sendEvent(emitter, data);
                if (isFinal) {
                    emitter.complete();
                }
            } catch (Exception e) {
                removeEmitter(scanId, emitter);
            }
        }

        if (isFinal) {
            emitters.remove(scanId);
        }
    }

    private void sendEvent(SseEmitter emitter, Object data) {
        try {
            String json = om.writeValueAsString(data);
            emitter.send(SseEmitter.event().data(json));
        } catch (IOException e) {
            log.debug("Failed to send SSE event: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void removeEmitter(UUID scanId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(scanId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emitters.remove(scanId);
            }
        }
    }

    public static String getCurrentTestName(AccessibilityScan scan) {
        if (scan.getStatus() == ScanStatus.COMPLETED) return "완료";
        if (scan.getStatus() == ScanStatus.FAILED) return "실패";
        if (scan.getStatus() == ScanStatus.PENDING) return "대기 중";

        if (!scan.isAltTextCompleted()) return "이미지 대체 텍스트 검사";
        if (!scan.isContrastCompleted()) return "색상 대비 검사";
        if (!scan.isKeyboardCompleted()) return "키보드 접근성 검사";
        if (!scan.isLabelCompleted()) return "레이블 검사";
        if (!scan.isTableCompleted()) return "표 구조 검사";
        if (!scan.isVideoCaptionCompleted() && !scan.isVideoAutoplayCompleted()) return "비디오 접근성 검사";
        if (!scan.isBasicLanguageCompleted()) return "기본 언어 검사";
        if (!scan.isMarkupErrorCompleted()) return "마크업 오류 검사";
        if (!scan.isHeadingCompleted()) return "제목 구조 검사";
        if (!scan.isResponseTimeCompleted()) return "응답시간 검사";
        if (!scan.isPauseControlCompleted()) return "정지 기능 검사";
        if (!scan.isFlashingCompleted()) return "깜빡임 검사";
        return "결과 처리 중";
    }

    public static String getProgressMessage(AccessibilityScan scan) {
        if (scan.getStatus() == ScanStatus.COMPLETED) {
            return "검사가 완료되었습니다. (총 " + scan.getTotalIssues() + "개 문제 발견)";
        }
        if (scan.getStatus() == ScanStatus.FAILED) {
            return "검사 중 오류가 발생했습니다: " + (scan.getErrorMessage() != null ? scan.getErrorMessage() : "");
        }
        if (scan.getStatus() == ScanStatus.PENDING) {
            return "검사 시작을 대기 중입니다.";
        }
        return getCurrentTestName(scan) + " 진행 중...";
    }
}
