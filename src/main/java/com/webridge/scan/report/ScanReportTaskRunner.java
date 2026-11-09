package com.webridge.scan.report;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class ScanReportTaskRunner {

    private final ScanReportService reportService;

    public ScanReportTaskRunner(@Lazy ScanReportService reportService) {
        this.reportService = reportService;
    }

    @Async("reportExecutor")
    public void runReportGeneration(UUID scanId, String format) {
        log.info("Starting async report generation for scan {}, format: {}", scanId, format);
        try {
            reportService.generateReportFile(scanId, format);
            log.info("Finished async report generation for scan {}", scanId);
        } catch (Exception e) {
            log.error("Failed to generate report for scan {}", scanId, e);
        }
    }
}
