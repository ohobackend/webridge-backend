package com.webridge.scan.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanCleanupScheduler {

    private final ScanCleanupService cleanupService;

    @Scheduled(fixedDelayString = "PT30M", initialDelayString = "PT5M")
    @SchedulerLock(name = "ScanCleanupScheduler_cleanupStuckScans", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    public void cleanupStuckScans() {
        try {
            int cleaned = cleanupService.cleanup(30);
            if (cleaned > 0) {
                log.info("Cleaned up {} stuck scans", cleaned);
            }
        } catch (Exception e) {
            log.warn("Scheduled cleanup of stuck scans failed: {}", e.getMessage());
        }
    }
}
