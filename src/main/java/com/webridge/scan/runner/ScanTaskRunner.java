package com.webridge.scan.runner;

import com.webridge.scan.checker.AccessibilityChecker;
import com.webridge.scan.checker.AccessibilityCheckerFactory;
import com.webridge.scan.checker.ScanResultSaver;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.progress.ScanProgressPublisher;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.survey.SurveyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanTaskRunner {

    private final AccessibilityScanRepository scanRepo;
    private final ScanResultSaver resultSaver;
    private final ScanProgressPublisher progress;
    private final AccessibilityCheckerFactory checkerFactory;
    private final SurveyService surveyService;

    @Async("scanExecutor")
    public void run(UUID scanId) {
        MDC.put("taskId", "scan-" + scanId);
        try {
            AccessibilityScan scan = scanRepo.findById(scanId).orElse(null);
            if (scan == null) {
                log.error("Scan {} not found", scanId);
                return;
            }

            if (scan.getUser() == null || !scan.getUser().isActive()) {
                log.warn("User for scan {} is deleted or inactive", scanId);
                resultSaver.cancel(scanId, "사용자가 삭제되었거나 비활성화되었습니다.");
                progress.error(scanId, "사용자가 삭제되었거나 비활성화되었습니다.");
                return;
            }

            resultSaver.resetForRun(scanId);
            progress.update(scanId, 0, "검사 시작을 준비 중입니다.", "검사 준비");

            try (AccessibilityChecker checker = checkerFactory.create(scan.getUrl())) {
                // Page Title
                try {
                    String title = checker.extractPageTitle();
                    resultSaver.saveTitleIfEmpty(scanId, title);
                } catch (Exception e) {
                    log.warn("Extract page title failed for scan {}: {}", scanId, e.getMessage());
                }

                // 1. Alt text
                progress.update(scanId, 10, "이미지 대체 텍스트 검사 시작", "이미지 대체 텍스트 검사");
                List<Map<String, Object>> altResults = checker.checkAltText();
                resultSaver.saveAltText(scanId, altResults);
                progress.update(scanId, 25, "이미지 대체 텍스트 검사 완료 (" + altResults.size() + "개 이미지 검사)", null);

                // 2. Contrast
                progress.update(scanId, 30, "색상 대비 검사 시작", "색상 대비 검사");
                List<Map<String, Object>> contrastResults = checker.checkContrast();
                resultSaver.saveContrast(scanId, contrastResults);
                progress.update(scanId, 45, "색상 대비 검사 완료 (" + contrastResults.size() + "개 요소 검사)", null);

                // 3. Keyboard
                progress.update(scanId, 50, "키보드 접근성 검사 시작", "키보드 접근성 검사");
                List<Map<String, Object>> keyboardResults = checker.checkKeyboard();
                resultSaver.saveKeyboard(scanId, keyboardResults);
                progress.update(scanId, 65, "키보드 접근성 검사 완료 (" + keyboardResults.size() + "개 요소 검사)", null);

                // 4. Labels
                progress.update(scanId, 70, "레이블 검사 시작", "레이블 검사");
                List<Map<String, Object>> labelResults = checker.checkLabels();
                resultSaver.saveLabels(scanId, labelResults);
                progress.update(scanId, 80, "레이블 검사 완료 (" + labelResults.size() + "개 입력 요소 검사)", null);

                // 5. Tables
                progress.update(scanId, 85, "표 구조 검사 시작", "표 구조 검사");
                List<Map<String, Object>> tableResults = checker.checkTables();
                resultSaver.saveTables(scanId, tableResults);
                progress.update(scanId, 90, "표 구조 검사 완료 (" + tableResults.size() + "개 표 검사)", null);

                // 6. Video Captions
                progress.update(scanId, 95, "비디오 자막 제공 검사 시작", "비디오 자막 제공 검사");
                List<Map<String, Object>> videoCaptionResults = checker.checkVideoCaptions();
                resultSaver.saveVideoCaptions(scanId, videoCaptionResults);
                progress.update(scanId, 96, "비디오 자막 검사 완료 (" + videoCaptionResults.size() + "개 비디오 검사)", null);

                // 7. Video Autoplay
                progress.update(scanId, 97, "비디오 자동 재생 금지 검사 시작", "비디오 자동 재생 금지 검사");
                List<Map<String, Object>> videoAutoplayResults = checker.checkVideoAutoplay();
                resultSaver.saveVideoAutoplay(scanId, videoAutoplayResults);
                progress.update(scanId, 98, "비디오 자동재생 검사 완료 (" + videoAutoplayResults.size() + "개 비디오 검사)", null);

                // 8. Basic Language
                progress.update(scanId, 98, "기본 언어 검사 시작", "기본 언어 검사");
                List<Map<String, Object>> basicLanguageResults = checker.checkBasicLanguage();
                resultSaver.saveBasicLanguage(scanId, basicLanguageResults);
                progress.update(scanId, 98, "기본 언어 검사 완료", null);

                // 9. Markup Error
                progress.update(scanId, 98, "마크업 오류 검사 시작", "마크업 오류 검사");
                List<Map<String, Object>> markupResults = checker.checkMarkupErrors();
                resultSaver.saveMarkupErrors(scanId, markupResults);
                progress.update(scanId, 98, "마크업 오류 검사 완료", null);

                // 10. Headings
                progress.update(scanId, 98, "제목 제공 검사 시작", "제목 제공 검사");
                List<Map<String, Object>> headingResults = checker.checkHeadings();
                resultSaver.saveHeadings(scanId, headingResults);
                progress.update(scanId, 98, "제목 제공 검사 완료", null);

                // 11. Response Time
                progress.update(scanId, 99, "응답시간 검사 시작", "응답시간 검사");
                List<Map<String, Object>> responseTimeResults = checker.checkResponseTime();
                resultSaver.saveResponseTime(scanId, responseTimeResults);
                progress.update(scanId, 99, "응답시간 검사 완료", null);

                // 12. Pause Control
                progress.update(scanId, 99, "정지 기능 검사 시작", "정지 기능 검사");
                List<Map<String, Object>> pauseResults = checker.checkPauseControl();
                resultSaver.savePauseControl(scanId, pauseResults);
                progress.update(scanId, 99, "정지 기능 검사 완료", null);

                // 13. Flashing
                progress.update(scanId, 99, "깜빡임 검사 시작", "깜빡임 검사");
                List<Map<String, Object>> flashingResults = checker.checkFlashing();
                resultSaver.saveFlashing(scanId, flashingResults);
                progress.update(scanId, 99, "깜빡임 검사 완료", null);

                // Complete
                resultSaver.complete(scanId);
                surveyService.onScanCompleted(scanId);

                AccessibilityScan completedScan = scanRepo.findById(scanId).orElse(scan);
                String completeMsg = String.format("검사 완료! 총 %d개 문제 발견 (준수율: %.1f%%)",
                        completedScan.getTotalIssues(), completedScan.getComplianceScore());
                progress.update(scanId, 100, completeMsg, null);
                log.info("Accessibility scan {} completed successfully", scanId);

            } catch (Exception e) {
                log.error("Accessibility scan {} failed: {}", scanId, e.getMessage(), e);
                resultSaver.fail(scanId, e.getMessage());
                progress.error(scanId, e.getMessage());
            }
        } finally {
            MDC.remove("taskId");
        }
    }
}
