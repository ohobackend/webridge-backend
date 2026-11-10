package com.webridge.scan.checker;

import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.AltTextResult;
import com.webridge.scan.entity.BasicLanguageResult;
import com.webridge.scan.entity.ContrastResult;
import com.webridge.scan.entity.FlashingResult;
import com.webridge.scan.entity.HeadingResult;
import com.webridge.scan.entity.KeyboardResult;
import com.webridge.scan.entity.LabelResult;
import com.webridge.scan.entity.MarkupErrorResult;
import com.webridge.scan.entity.PauseControlResult;
import com.webridge.scan.entity.ResponseTimeResult;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.entity.TableResult;
import com.webridge.scan.entity.VideoAutoplayResult;
import com.webridge.scan.entity.VideoCaptionResult;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanResultSaver {

    private final AccessibilityScanRepository scanRepo;
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
    public void resetForRun(UUID scanId) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        scan.resetForRun();
        scanRepo.save(scan);
    }

    @Transactional
    public void saveTitleIfEmpty(UUID scanId, String title) {
        if (title == null || title.isBlank()) return;
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        if (scan.getTitle() == null || scan.getTitle().isBlank()) {
            scan.setTitle(title.length() > 200 ? title.substring(0, 200) : title);
            scanRepo.save(scan);
        }
    }

    @Transactional
    public void saveAltText(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            String imgUrl = (String) r.getOrDefault("img_url", "N/A");
            String elementHtml = (String) r.getOrDefault("element_html", "");
            String altText = (String) r.get("alt_text");
            String answer = (String) r.getOrDefault("answer", "");
            int compliance = r.get("compliant") instanceof Number n ? n.intValue() : 3;
            String aiType = (String) r.getOrDefault("ai_type", "");
            String aiGrade = (String) r.getOrDefault("ai_grade", "");
            String aiReason = (String) r.getOrDefault("ai_reason", "");
            String aiImprovement = (String) r.getOrDefault("ai_improvement", "");
            boolean hasError = Boolean.TRUE.equals(r.get("has_error"));

            AltTextResult entity = AltTextResult.builder()
                    .scan(scan)
                    .imgUrl(imgUrl)
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .altText(altText)
                    .suggestedAlt(answer != null ? answer : "")
                    .compliance(compliance)
                    .aiType(aiType != null ? aiType : "")
                    .aiGrade(aiGrade != null ? aiGrade : "")
                    .aiReason(aiReason != null ? aiReason : "")
                    .aiImprovement(aiImprovement != null ? aiImprovement : "")
                    .hasError(hasError)
                    .build();
            altTextRepo.save(entity);
            scan.getAltTextResults().add(entity);
        }
        scan.setAltTextCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveContrast(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("summary") || r.containsKey("error")) continue;
            String text = (String) r.getOrDefault("text", "");
            String elementHtml = (String) r.getOrDefault("element_html", "");
            String fgColor = (String) r.getOrDefault("foreground_color", "");
            String bgColor = (String) r.getOrDefault("background_color", "");
            double contrastRatio = r.get("contrast_ratio") instanceof Number n ? n.doubleValue() : 0.0;
            boolean wcagCompliant = Boolean.TRUE.equals(r.get("wcag_compliant"));

            ContrastResult entity = ContrastResult.builder()
                    .scan(scan)
                    .textContent(text != null ? text : "")
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .foregroundColor(fgColor != null ? fgColor : "")
                    .backgroundColor(bgColor != null ? bgColor : "")
                    .contrastRatio(contrastRatio)
                    .wcagCompliant(wcagCompliant)
                    .build();
            contrastRepo.save(entity);
            scan.getContrastResults().add(entity);
        }
        scan.setContrastCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveKeyboard(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("summary") || r.containsKey("error")) continue;
            String element = (String) r.getOrDefault("element", "");
            String elementHtml = (String) r.getOrDefault("element_html", "");
            boolean accessible = Boolean.TRUE.equals(r.get("accessible"));
            String message = (String) r.getOrDefault("message", "");

            KeyboardResult entity = KeyboardResult.builder()
                    .scan(scan)
                    .elementType(element != null ? element : "")
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .accessible(accessible)
                    .message(message != null ? message : "")
                    .build();
            keyboardRepo.save(entity);
            scan.getKeyboardResults().add(entity);
        }
        scan.setKeyboardCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveLabels(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("summary") || r.containsKey("error")) continue;
            String inputType = (String) r.getOrDefault("input_type", "");
            String elementHtml = (String) r.getOrDefault("element_html", "");
            boolean labelPresent = Boolean.TRUE.equals(r.get("label_present"));
            String message = (String) r.getOrDefault("message", "");

            LabelResult entity = LabelResult.builder()
                    .scan(scan)
                    .inputType(inputType != null ? inputType : "")
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .labelPresent(labelPresent)
                    .message(message != null ? message : "")
                    .build();
            labelRepo.save(entity);
            scan.getLabelResults().add(entity);
        }
        scan.setLabelCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveTables(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("summary") || r.containsKey("error")) continue;
            int headers = r.get("headers") instanceof Number n ? n.intValue() : 0;
            int rows = r.get("rows") instanceof Number n ? n.intValue() : 0;
            String elementHtml = (String) r.getOrDefault("element_html", "");
            boolean compliant = Boolean.TRUE.equals(r.get("compliant"));
            String message = (String) r.getOrDefault("message", "");

            TableResult entity = TableResult.builder()
                    .scan(scan)
                    .headersCount(headers)
                    .rowsCount(rows)
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .compliant(compliant)
                    .message(message != null ? message : "")
                    .build();
            tableRepo.save(entity);
            scan.getTableResults().add(entity);
        }
        scan.setTableCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveVideoCaptions(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("error")) continue;
            String videoUrl = (String) r.getOrDefault("video_url", "");
            String elementHtml = (String) r.getOrDefault("element_html", "");
            boolean hasThumbnail = Boolean.TRUE.equals(r.get("has_thumbnail"));
            boolean hasTranscript = Boolean.TRUE.equals(r.get("has_transcript"));
            boolean hasAudioDescription = Boolean.TRUE.equals(r.get("has_audio_description"));
            boolean hasAriaLabel = Boolean.TRUE.equals(r.get("has_aria_label"));
            boolean compliant = Boolean.TRUE.equals(r.get("compliant"));
            String message = (String) r.getOrDefault("message", "");

            VideoCaptionResult entity = VideoCaptionResult.builder()
                    .scan(scan)
                    .videoUrl(videoUrl != null ? videoUrl : "")
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .hasThumbnail(hasThumbnail)
                    .hasTranscript(hasTranscript)
                    .hasAudioDescription(hasAudioDescription)
                    .hasAriaLabel(hasAriaLabel)
                    .compliant(compliant)
                    .message(message != null ? message : "")
                    .build();
            videoCaptionRepo.save(entity);
            scan.getVideoCaptionResults().add(entity);
        }
        scan.setVideoCaptionCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveVideoAutoplay(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("error")) continue;
            String videoUrl = (String) r.getOrDefault("video_url", "");
            String elementHtml = (String) r.getOrDefault("element_html", "");
            boolean autoplayDisabled = Boolean.TRUE.equals(r.get("autoplay_disabled"));
            boolean keyboardAccessible = Boolean.TRUE.equals(r.get("keyboard_accessible"));
            boolean compliant = Boolean.TRUE.equals(r.get("compliant"));
            String message = (String) r.getOrDefault("message", "");

            VideoAutoplayResult entity = VideoAutoplayResult.builder()
                    .scan(scan)
                    .videoUrl(videoUrl != null ? videoUrl : "")
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .autoplayDisabled(autoplayDisabled)
                    .keyboardAccessible(keyboardAccessible)
                    .compliant(compliant)
                    .message(message != null ? message : "")
                    .build();
            videoAutoplayRepo.save(entity);
            scan.getVideoAutoplayResults().add(entity);
        }
        scan.setVideoAutoplayCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveBasicLanguage(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("error")) continue;
            String lang = (String) r.getOrDefault("lang_attribute", "");
            boolean compliant = Boolean.TRUE.equals(r.get("compliant"));
            String message = (String) r.getOrDefault("message", "");

            BasicLanguageResult entity = BasicLanguageResult.builder()
                    .scan(scan)
                    .langAttribute(lang != null && lang.length() <= 10 ? lang : (lang != null ? lang.substring(0, 10) : ""))
                    .compliant(compliant)
                    .message(message != null ? message : "")
                    .build();
            basicLanguageRepo.save(entity);
            scan.getBasicLanguageResults().add(entity);
        }
        scan.setBasicLanguageCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveMarkupErrors(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("error")) continue;
            String errorType = (String) r.get("error_type");
            String errorDetail = (String) r.get("error_detail");
            String elementId = (String) r.get("element_id");
            String elementHtml = (String) r.get("element_html");
            boolean compliant = Boolean.TRUE.equals(r.get("compliant"));
            String message = (String) r.getOrDefault("message", "");

            MarkupErrorResult entity = MarkupErrorResult.builder()
                    .scan(scan)
                    .errorType(errorType)
                    .errorDetail(errorDetail)
                    .elementId(elementId)
                    .elementHtml(elementHtml)
                    .compliant(compliant)
                    .message(message)
                    .build();
            markupErrorRepo.save(entity);
            scan.getMarkupErrorResults().add(entity);
        }
        scan.setMarkupErrorCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public void saveHeadings(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("error")) continue;
            Map<String, Object> pageTitle = (Map<String, Object>) r.getOrDefault("page_title", Map.of());
            Map<String, Object> popupFrames = (Map<String, Object>) r.getOrDefault("popup_frames", Map.of());
            Map<String, Object> frames = (Map<String, Object>) r.getOrDefault("frames", Map.of());
            Map<String, Object> contentHeadings = (Map<String, Object>) r.getOrDefault("content_headings", Map.of());
            Map<String, Object> semanticSections = (Map<String, Object>) r.getOrDefault("semantic_sections", Map.of());
            int specialSymbols = r.get("special_symbol_issues") instanceof Number n ? n.intValue() : 0;
            boolean compliant = Boolean.TRUE.equals(r.get("compliant"));
            String message = (String) r.getOrDefault("message", "");
            String elementHtml = (String) r.getOrDefault("element_html", "");
            List<Object> rawIssues = (List<Object>) r.getOrDefault("issues", List.of());
            Map<String, Object> details = (Map<String, Object>) r.getOrDefault("details", Map.of());

            List<Map<String, Object>> issuesDetails = new ArrayList<>();
            for (Object obj : rawIssues) {
                if (obj instanceof String s) {
                    issuesDetails.add(Map.of("issue", s));
                } else if (obj instanceof Map<?, ?> m) {
                    issuesDetails.add((Map<String, Object>) m);
                }
            }

            boolean pageTitleExists = Boolean.TRUE.equals(pageTitle.get("exists"));
            String pageTitleText = (String) pageTitle.getOrDefault("text", "");
            int pageTitleLength = pageTitle.get("length") instanceof Number n ? n.intValue() : 0;

            String complianceRate = (String) r.getOrDefault("compliance_rate", "");
            if (complianceRate == null || complianceRate.isBlank()) {
                complianceRate = compliant ? "100%" : "0%";
            }

            HeadingResult entity = HeadingResult.builder()
                    .scan(scan)
                    .complianceRate(complianceRate)
                    .pageTitleExists(pageTitleExists)
                    .pageTitleEmpty(!pageTitleExists || pageTitleText.isBlank())
                    .pageTitleText(pageTitleText)
                    .pageTitleLength(pageTitleLength)
                    .totalPopups(popupFrames.get("total") instanceof Number n ? n.intValue() : 0)
                    .popupsWithoutTitle(popupFrames.get("without_title") instanceof Number n ? n.intValue() : 0)
                    .totalFrames(frames.get("total") instanceof Number n ? n.intValue() : 0)
                    .framesWithoutTitle(frames.get("without_title") instanceof Number n ? n.intValue() : 0)
                    .totalHeadings(contentHeadings.get("total") instanceof Number n ? n.intValue() : 0)
                    .emptyHeadings(contentHeadings.get("empty") instanceof Number n ? n.intValue() : 0)
                    .headingsWithoutContent(contentHeadings.get("without_content") instanceof Number n ? n.intValue() : 0)
                    .totalSemanticSections(semanticSections.get("total") instanceof Number n ? n.intValue() : 0)
                    .sectionsWithoutHeadings(semanticSections.get("without_headings") instanceof Number n ? n.intValue() : 0)
                    .specialSymbolIssues(specialSymbols)
                    .issuesDetails(issuesDetails)
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .compliant(compliant)
                    .message(message != null ? message : "")
                    .detailedResults(details != null ? details : Map.of())
                    .build();
            entity.calculateTotalIssues();
            headingRepo.save(entity);
            scan.getHeadingResults().add(entity);
        }
        scan.setHeadingCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveResponseTime(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("error")) continue;
            boolean hasTimeLimit = Boolean.TRUE.equals(r.get("has_time_limit"));
            int shortTimeouts = r.get("short_timeouts") instanceof Number n ? n.intValue() : 0;
            String elementHtml = (String) r.getOrDefault("element_html", "");
            boolean compliant = Boolean.TRUE.equals(r.get("compliant"));
            String message = (String) r.getOrDefault("message", "");

            ResponseTimeResult entity = ResponseTimeResult.builder()
                    .scan(scan)
                    .hasTimeLimit(hasTimeLimit)
                    .shortTimeouts(shortTimeouts)
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .compliant(compliant)
                    .message(message != null ? message : "")
                    .build();
            responseTimeRepo.save(entity);
            scan.getResponseTimeResults().add(entity);
        }
        scan.setResponseTimeCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void savePauseControl(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("error")) continue;
            String tag = (String) r.get("tag");
            String elementHtml = (String) r.getOrDefault("element_html", "");
            boolean hasAutoplay = Boolean.TRUE.equals(r.get("has_autoplay"));
            boolean hasPause = Boolean.TRUE.equals(r.get("has_pause"));
            boolean compliant = Boolean.TRUE.equals(r.get("compliant"));
            String message = (String) r.getOrDefault("message", "");

            PauseControlResult entity = PauseControlResult.builder()
                    .scan(scan)
                    .tag(tag)
                    .elementHtml(elementHtml != null ? elementHtml : "")
                    .hasAutoplay(hasAutoplay)
                    .hasPause(hasPause)
                    .compliant(compliant)
                    .message(message != null ? message : "")
                    .build();
            pauseControlRepo.save(entity);
            scan.getPauseControlResults().add(entity);
        }
        scan.setPauseControlCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void saveFlashing(UUID scanId, List<Map<String, Object>> results) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        for (Map<String, Object> r : results) {
            if (r.containsKey("error")) continue;
            String flashingType = (String) r.get("flashing_type");
            String elementHtml = (String) r.get("element_html");
            Integer duration = r.get("duration") instanceof Number n ? n.intValue() : null;
            boolean compliant = Boolean.TRUE.equals(r.get("compliant"));
            String message = (String) r.getOrDefault("message", "");

            FlashingResult entity = FlashingResult.builder()
                    .scan(scan)
                    .flashingType(flashingType)
                    .elementHtml(elementHtml)
                    .duration(duration)
                    .compliant(compliant)
                    .message(message != null ? message : "")
                    .build();
            flashingRepo.save(entity);
            scan.getFlashingResults().add(entity);
        }
        scan.setFlashingCompleted(true);
        scanRepo.save(scan);
    }

    @Transactional
    public void complete(UUID scanId) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        int totalIssues = ScoreCalculator.calculateTotalIssues(scan);
        double complianceScore = ScoreCalculator.calculateComplianceScore(scan);

        scan.setTotalIssues(totalIssues);
        scan.setComplianceScore(complianceScore);
        scan.setStatus(ScanStatus.COMPLETED);
        scan.setProgress(100);
        scan.setCompletedAt(OffsetDateTime.now());
        scanRepo.save(scan);
    }

    @Transactional
    public void fail(UUID scanId, String errorMessage) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        scan.setStatus(ScanStatus.FAILED);
        scan.setProgress(-1);
        scan.setErrorMessage(errorMessage != null ? errorMessage : "Scan failed");
        scanRepo.save(scan);
    }

    @Transactional
    public void cancel(UUID scanId, String reason) {
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        scan.setStatus(ScanStatus.CANCELLED);
        scan.setErrorMessage(reason != null ? reason : "Scan cancelled");
        scanRepo.save(scan);
    }
}
