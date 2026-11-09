package com.webridge.scan.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.scan.entity.AccessibilityScan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonReportGenerator {

    private final ObjectMapper om;

    public void generate(AccessibilityScan scan, File outputFile) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", scan.getId().toString());
        info.put("url", scan.getUrl());
        info.put("title", scan.getTitle() != null ? scan.getTitle() : "");
        info.put("created_at", scan.getCreatedAt() != null ? scan.getCreatedAt().toString() : null);
        info.put("completed_at", scan.getCompletedAt() != null ? scan.getCompletedAt().toString() : null);
        info.put("total_issues", scan.getTotalIssues());
        info.put("compliance_score", scan.getComplianceScore());
        root.put("scan_info", info);

        Map<String, Object> results = new LinkedHashMap<>();

        // 1. Alt Text
        results.put("alt_text", scan.getAltTextResults() != null ? scan.getAltTextResults().stream().map(r -> Map.of(
                "img_url", r.getImgUrl() != null ? r.getImgUrl() : "",
                "alt_text", r.getAltText() != null ? r.getAltText() : "",
                "suggested_alt", r.getSuggestedAlt() != null ? r.getSuggestedAlt() : "",
                "compliance", r.getCompliance(),
                "compliance_display", mapComplianceDisplay(r.getCompliance())
        )).toList() : List.of());

        // 2. Contrast
        results.put("contrast", scan.getContrastResults() != null ? scan.getContrastResults().stream().map(r -> Map.of(
                "text_content", r.getTextContent() != null ? r.getTextContent() : "",
                "foreground_color", r.getForegroundColor() != null ? r.getForegroundColor() : "",
                "background_color", r.getBackgroundColor() != null ? r.getBackgroundColor() : "",
                "contrast_ratio", r.getContrastRatio(),
                "wcag_compliant", r.isWcagCompliant()
        )).toList() : List.of());

        // 3. Keyboard
        results.put("keyboard", scan.getKeyboardResults() != null ? scan.getKeyboardResults().stream().map(r -> Map.of(
                "element_type", r.getElementType() != null ? r.getElementType() : "",
                "accessible", r.isAccessible(),
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        // 4. Label
        results.put("label", scan.getLabelResults() != null ? scan.getLabelResults().stream().map(r -> Map.of(
                "input_type", r.getInputType() != null ? r.getInputType() : "",
                "label_present", r.isLabelPresent(),
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        // 5. Table
        results.put("table", scan.getTableResults() != null ? scan.getTableResults().stream().map(r -> Map.of(
                "headers_count", r.getHeadersCount(),
                "rows_count", r.getRowsCount(),
                "compliant", r.isCompliant(),
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        // 6. Video Caption
        results.put("video_caption", scan.getVideoCaptionResults() != null ? scan.getVideoCaptionResults().stream().map(r -> Map.of(
                "video_url", r.getVideoUrl() != null ? r.getVideoUrl() : "",
                "has_transcript", r.isHasTranscript(),
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        // 7. Video Autoplay
        results.put("video_autoplay", scan.getVideoAutoplayResults() != null ? scan.getVideoAutoplayResults().stream().map(r -> Map.of(
                "video_url", r.getVideoUrl() != null ? r.getVideoUrl() : "",
                "autoplay_disabled", r.isAutoplayDisabled(),
                "keyboard_accessible", r.isKeyboardAccessible()
        )).toList() : List.of());

        // 8. Basic Language
        results.put("basic_language", scan.getBasicLanguageResults() != null ? scan.getBasicLanguageResults().stream().map(r -> Map.of(
                "lang_attribute", r.getLangAttribute() != null ? r.getLangAttribute() : "",
                "compliant", r.isCompliant(),
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        // 9. Markup Error
        results.put("markup_error", scan.getMarkupErrorResults() != null ? scan.getMarkupErrorResults().stream().map(r -> Map.of(
                "error_type", r.getErrorType() != null ? r.getErrorType() : "",
                "error_detail", r.getErrorDetail() != null ? r.getErrorDetail() : "",
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        // 10. Heading
        results.put("heading", scan.getHeadingResults() != null ? scan.getHeadingResults().stream().map(r -> Map.of(
                "total_issues", r.getTotalIssues(),
                "compliant", r.isCompliant(),
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        // 11. Response Time
        results.put("response_time", scan.getResponseTimeResults() != null ? scan.getResponseTimeResults().stream().map(r -> Map.of(
                "compliant", r.isCompliant(),
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        // 12. Pause Control
        results.put("pause_control", scan.getPauseControlResults() != null ? scan.getPauseControlResults().stream().map(r -> Map.of(
                "compliant", r.isCompliant(),
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        // 13. Flashing
        results.put("flashing", scan.getFlashingResults() != null ? scan.getFlashingResults().stream().map(r -> Map.of(
                "compliant", r.isCompliant(),
                "message", r.getMessage() != null ? r.getMessage() : ""
        )).toList() : List.of());

        root.put("results", results);

        om.writerWithDefaultPrettyPrinter().writeValue(outputFile, root);
    }

    private String mapComplianceDisplay(int c) {
        return switch (c) {
            case 0 -> "정상";
            case 1 -> "개선필요";
            case 2 -> "미준수";
            default -> "오류";
        };
    }
}
