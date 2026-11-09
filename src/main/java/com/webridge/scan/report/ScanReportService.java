package com.webridge.scan.report;

import com.webridge.common.exception.ApiException;
import com.webridge.config.WebridgeProperties;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanReport;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.report.dto.ScanReportResponse;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.scan.repository.ScanReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanReportService {

    private static final Set<String> ALLOWED_FORMATS = Set.of("pdf", "excel", "json");

    private final AccessibilityScanRepository scanRepo;
    private final ScanReportRepository reportRepo;
    private final WebridgeProperties props;
    private final PdfReportGenerator pdfGen;
    private final ExcelReportGenerator excelGen;
    private final JsonReportGenerator jsonGen;
    private final ScanReportTaskRunner taskRunner;

    public ScanReportResponse requestReport(UUID scanId, Long userId, String format) {
        String fmt = (format != null && !format.isBlank()) ? format.trim().toLowerCase() : "pdf";
        if (!ALLOWED_FORMATS.contains(fmt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    Map.of("error", "지원하지 않는 리포트 형식입니다. (pdf, excel, json만 지원)"));
        }

        AccessibilityScan scan = scanRepo.findByIdAndUserId(scanId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        Map.of("detail", "검사를 찾을 수 없습니다.")));

        if (scan.getStatus() != ScanStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    Map.of("error", "완료된 검사만 리포트를 생성할 수 있습니다."));
        }

        UUID taskId = UUID.randomUUID();
        taskRunner.runReportGeneration(scanId, fmt);

        return new ScanReportResponse(
                taskId.toString(),
                "리포트 생성이 시작되었습니다.",
                fmt
        );
    }

    @Transactional
    public File generateReportFile(UUID scanId, String format) throws Exception {
        String fmt = format.trim().toLowerCase();
        AccessibilityScan scan = scanRepo.findById(scanId)
                .orElseThrow(() -> new IllegalArgumentException("Scan not found: " + scanId));

        if (scan.getStatus() != ScanStatus.COMPLETED) {
            throw new IllegalStateException("Scan is not completed: " + scanId);
        }

        File reportsDir = resolveReportsDir();
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        String ext = "excel".equals(fmt) ? "xlsx" : fmt;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "accessibility_report_" + scanId + "_" + timestamp + "." + ext;
        File outputFile = new File(reportsDir, filename);

        switch (fmt) {
            case "pdf" -> pdfGen.generate(scan, outputFile);
            case "excel" -> excelGen.generate(scan, outputFile);
            case "json" -> jsonGen.generate(scan, outputFile);
            default -> throw new IllegalArgumentException("Unsupported format: " + fmt);
        }

        String relPath = "reports/" + filename;

        Optional<ScanReport> existing = reportRepo.findByScanId(scanId);
        ScanReport report;
        if (existing.isPresent()) {
            report = existing.get();
            if (report.getFilePath() != null) {
                File oldFile = resolveFile(report.getFilePath());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }
            report.setFormat(fmt);
            report.setFilePath(relPath);
            report.setGeneratedAt(OffsetDateTime.now());
            report.setDownloadCount(0);
        } else {
            report = ScanReport.builder()
                    .scan(scan)
                    .format(fmt)
                    .filePath(relPath)
                    .generatedAt(OffsetDateTime.now())
                    .downloadCount(0)
                    .build();
        }
        reportRepo.save(report);

        log.info("Report generated successfully for scan {}: {}", scanId, outputFile.getAbsolutePath());
        return outputFile;
    }

    @Transactional
    public ResponseEntity<Resource> downloadReport(UUID scanId, Long userId) {
        scanRepo.findByIdAndUserId(scanId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        Map.of("detail", "검사를 찾을 수 없습니다.")));

        ScanReport report = reportRepo.findByScanId(scanId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        Map.of("error", "생성된 리포트가 없습니다. 먼저 리포트를 생성해주세요.")));

        if (report.getFilePath() == null || report.getFilePath().isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    Map.of("error", "생성된 리포트가 없습니다. 먼저 리포트를 생성해주세요."));
        }

        File file = resolveFile(report.getFilePath());
        if (!file.exists()) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    Map.of("error", "리포트 파일을 찾을 수 없습니다."));
        }

        report.setDownloadCount(report.getDownloadCount() + 1);
        reportRepo.save(report);

        Resource resource = new FileSystemResource(file);
        String ext = "excel".equalsIgnoreCase(report.getFormat()) ? "xlsx" : report.getFormat();
        String downloadFilename = "accessibility_report_" + scanId + "." + ext;

        MediaType mediaType = switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "json" -> MediaType.APPLICATION_JSON;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFilename + "\"")
                .contentLength(file.length())
                .body(resource);
    }

    private File resolveFile(String path) {
        File f = new File(path);
        if (f.exists()) {
            return f;
        }
        File inDir = new File(resolveReportsDir(), f.getName());
        if (inDir.exists()) {
            return inDir;
        }
        return f;
    }

    private File resolveReportsDir() {
        String dir = props.reportsDir();
        if (dir == null || dir.isBlank()) {
            dir = "./reports";
        }
        return new File(dir);
    }
}
