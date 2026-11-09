package com.webridge.scan.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.repository.AccessibilityScanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class ScanProgressPublisher {

    private final AccessibilityScanRepository scanRepo;
    private final ScanWebSocketHandler wsHandler;
    private final ScanSseService sseService;
    private final Optional<StringRedisTemplate> redisTemplate;
    private final ObjectMapper om;

    public ScanProgressPublisher(
            AccessibilityScanRepository scanRepo,
            ScanWebSocketHandler wsHandler,
            ScanSseService sseService,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper om) {
        this.scanRepo = scanRepo;
        this.wsHandler = wsHandler;
        this.sseService = sseService;
        this.redisTemplate = Optional.ofNullable(redisTemplate);
        this.om = om;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update(UUID scanId, int progress, String message, String currentTest) {
        final ScanStatus status;
        if (progress >= 100) {
            status = ScanStatus.COMPLETED;
        } else if (progress < 0) {
            status = ScanStatus.FAILED;
        } else {
            status = ScanStatus.PROCESSING;
        }

        scanRepo.findById(scanId).ifPresent(scan -> {
            scan.setProgress(Math.max(0, Math.min(100, progress)));
            scan.setStatus(status);
            scanRepo.save(scan);
        });

        boolean isFinal = (progress >= 100 || progress < 0);
        String eventType = isFinal ? "progress" : "progress";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scan_id", scanId.toString());
        data.put("status", status.getValue());
        data.put("progress", Math.max(0, Math.min(100, progress)));
        if (currentTest != null) {
            data.put("current_test", currentTest);
        }
        data.put("message", message);
        data.put("timestamp", OffsetDateTime.now().toString());
        data.put("type", eventType);
        if (isFinal) {
            data.put("final", true);
        }

        dispatch(scanId, data);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void error(UUID scanId, String errorMessage) {
        scanRepo.findById(scanId).ifPresent(scan -> {
            scan.setStatus(ScanStatus.FAILED);
            scan.setErrorMessage(errorMessage);
            scanRepo.save(scan);
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scan_id", scanId.toString());
        data.put("status", "failed");
        data.put("error", errorMessage);
        data.put("message", "검사 중 오류가 발생했습니다: " + errorMessage);
        data.put("timestamp", OffsetDateTime.now().toString());
        data.put("type", "error");
        data.put("final", true);

        dispatch(scanId, data);
        wsHandler.broadcastError(scanId, data);
    }

    private void dispatch(UUID scanId, Map<String, Object> data) {
        // 1. WebSocket
        try {
            wsHandler.broadcast(scanId, data);
        } catch (Exception e) {
            log.warn("WebSocket dispatch failed for {}: {}", scanId, e.getMessage());
        }

        // 2. SSE
        try {
            sseService.emitProgress(scanId, data);
        } catch (Exception e) {
            log.warn("SSE dispatch failed for {}: {}", scanId, e.getMessage());
        }

        // 3. Redis Pub/Sub (optional)
        if (redisTemplate.isPresent()) {
            try {
                String payload = om.writeValueAsString(Map.of("scan_id", scanId.toString(), "data", data));
                redisTemplate.get().convertAndSend("scan-progress", payload);
            } catch (Exception ignored) {}
        }
    }
}
