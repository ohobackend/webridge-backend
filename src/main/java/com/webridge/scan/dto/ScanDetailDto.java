package com.webridge.scan.dto;

import com.webridge.alttextfeedback.entity.AltTextFeedback;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.HeadingResult;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ScanDetailDto(
        UUID id,
        String url,
        String title,
        String status,
        String statusDisplay,
        int progress,
        int totalIssues,
        double complianceScore,
        int completionPercentage,

        boolean altTextCompleted,
        boolean contrastCompleted,
        boolean keyboardCompleted,
        boolean labelCompleted,
        boolean tableCompleted,
        boolean videoCaptionCompleted,
        boolean videoAutoplayCompleted,
        boolean basicLanguageCompleted,
        boolean markupErrorCompleted,
        boolean headingCompleted,
        boolean responseTimeCompleted,
        boolean pauseControlCompleted,
        boolean flashingCompleted,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime completedAt,
        String errorMessage,

        List<AltTextItemDto> altTextResults,
        List<ContrastItemDto> contrastResults,
        List<KeyboardItemDto> keyboardResults,
        List<LabelItemDto> labelResults,
        List<TableItemDto> tableResults,
        List<VideoCaptionItemDto> videoCaptionResults,
        List<VideoAutoplayItemDto> videoAutoplayResults,
        List<BasicLanguageItemDto> basicLanguageResults,
        List<MarkupErrorItemDto> markupErrorResults,
        List<HeadingItemDto> headingResults,
        List<ResponseTimeItemDto> responseTimeResults,
        List<PauseControlItemDto> pauseControlResults,
        List<FlashingItemDto> flashingResults,

        ScanReportItemDto report,
        List<ScanCategorySummary> scanSummary
) {
    public static ScanDetailDto from(AccessibilityScan scan, Map<Long, String> userFeedbackMap, String baseUrl) {
        return from(scan, userFeedbackMap, baseUrl, null);
    }

    public static ScanDetailDto from(
            AccessibilityScan scan,
            Map<Long, String> userFeedbackMap,
            String baseUrl,
            com.webridge.scan.entity.ScanReport report
    ) {
        String statusVal = scan.getStatus() != null ? scan.getStatus().getValue() : "pending";
        String statusDisplay = switch (statusVal) {
            case "processing" -> "진행중";
            case "completed" -> "완료";
            case "failed" -> "실패";
            case "cancelled" -> "취소";
            default -> "대기중";
        };

        var altItems = (scan.getAltTextResults() != null)
                ? scan.getAltTextResults().stream()
                .map(r -> AltTextItemDto.from(r, userFeedbackMap != null ? userFeedbackMap.get(r.getId()) : null))
                .toList()
                : List.<AltTextItemDto>of();

        var contrastItems = (scan.getContrastResults() != null)
                ? scan.getContrastResults().stream().map(ContrastItemDto::from).toList()
                : List.<ContrastItemDto>of();

        var keyboardItems = (scan.getKeyboardResults() != null)
                ? scan.getKeyboardResults().stream().map(KeyboardItemDto::from).toList()
                : List.<KeyboardItemDto>of();

        var labelItems = (scan.getLabelResults() != null)
                ? scan.getLabelResults().stream().map(LabelItemDto::from).toList()
                : List.<LabelItemDto>of();

        var tableItems = (scan.getTableResults() != null)
                ? scan.getTableResults().stream().map(TableItemDto::from).toList()
                : List.<TableItemDto>of();

        var captionItems = (scan.getVideoCaptionResults() != null)
                ? scan.getVideoCaptionResults().stream().map(VideoCaptionItemDto::from).toList()
                : List.<VideoCaptionItemDto>of();

        var autoplayItems = (scan.getVideoAutoplayResults() != null)
                ? scan.getVideoAutoplayResults().stream().map(VideoAutoplayItemDto::from).toList()
                : List.<VideoAutoplayItemDto>of();

        var langItems = (scan.getBasicLanguageResults() != null)
                ? scan.getBasicLanguageResults().stream().map(BasicLanguageItemDto::from).toList()
                : List.<BasicLanguageItemDto>of();

        var markupItems = (scan.getMarkupErrorResults() != null)
                ? scan.getMarkupErrorResults().stream().map(MarkupErrorItemDto::from).toList()
                : List.<MarkupErrorItemDto>of();

        var headingItems = (scan.getHeadingResults() != null)
                ? scan.getHeadingResults().stream().map(HeadingItemDto::from).toList()
                : List.<HeadingItemDto>of();

        var responseTimeItems = (scan.getResponseTimeResults() != null)
                ? scan.getResponseTimeResults().stream().map(ResponseTimeItemDto::from).toList()
                : List.<ResponseTimeItemDto>of();

        var pauseItems = (scan.getPauseControlResults() != null)
                ? scan.getPauseControlResults().stream().map(PauseControlItemDto::from).toList()
                : List.<PauseControlItemDto>of();

        var flashingItems = (scan.getFlashingResults() != null)
                ? scan.getFlashingResults().stream().map(FlashingItemDto::from).toList()
                : List.<FlashingItemDto>of();

        List<ScanCategorySummary> summaries = buildScanSummary(scan);

        return new ScanDetailDto(
                scan.getId(),
                scan.getUrl(),
                scan.getTitle() != null ? scan.getTitle() : "",
                statusVal,
                statusDisplay,
                scan.getProgress(),
                scan.getTotalIssues(),
                scan.getComplianceScore(),
                (int) Math.round(scan.getCompletionPercentage()),

                scan.isAltTextCompleted(),
                scan.isContrastCompleted(),
                scan.isKeyboardCompleted(),
                scan.isLabelCompleted(),
                scan.isTableCompleted(),
                scan.isVideoCaptionCompleted(),
                scan.isVideoAutoplayCompleted(),
                scan.isBasicLanguageCompleted(),
                scan.isMarkupErrorCompleted(),
                scan.isHeadingCompleted(),
                scan.isResponseTimeCompleted(),
                scan.isPauseControlCompleted(),
                scan.isFlashingCompleted(),

                scan.getCreatedAt(),
                scan.getUpdatedAt(),
                scan.getCompletedAt(),
                scan.getErrorMessage(),

                altItems,
                contrastItems,
                keyboardItems,
                labelItems,
                tableItems,
                captionItems,
                autoplayItems,
                langItems,
                markupItems,
                headingItems,
                responseTimeItems,
                pauseItems,
                flashingItems,

                report != null ? ScanReportItemDto.from(report, baseUrl) : null,
                summaries
        );
    }

    public static List<ScanCategorySummary> buildScanSummary(AccessibilityScan scan) {
        List<ScanCategorySummary> summary = new ArrayList<>();

        // 1. 적절한 대체 텍스트 제공
        long altCount = scan.getAltTextResults() != null ? scan.getAltTextResults().size() : 0;
        long altIssues = scan.getAltTextResults() != null ? scan.getAltTextResults().stream()
                .filter(r -> r.getCompliance() == 1 || r.getCompliance() == 2).count() : 0;
        double altPct = altCount > 0 ? ((altCount - altIssues) / (double) altCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(1, "적절한 대체 텍스트 제공", altIssues + "/" + altCount, round(altPct), altIssues > 0 ? "오류 확인" : "Cell Text"));

        // 2. 자막 제공
        long videoCount = scan.getVideoCaptionResults() != null ? scan.getVideoCaptionResults().size() : 0;
        long videoIssues = scan.getVideoCaptionResults() != null ? scan.getVideoCaptionResults().stream()
                .filter(r -> !r.isCompliant()).count() : 0;
        double videoPct = videoCount > 0 ? ((videoCount - videoIssues) / (double) videoCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(2, "자막 제공", "Cell Text", round(videoPct), "Cell Text"));

        // 3. 표의 구성
        long tableCount = scan.getTableResults() != null ? scan.getTableResults().size() : 0;
        long tableIssues = scan.getTableResults() != null ? scan.getTableResults().stream()
                .filter(r -> !r.isCompliant()).count() : 0;
        double tablePct = tableCount > 0 ? ((tableCount - tableIssues) / (double) tableCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(3, "표의 구성", "Cell Text", round(tablePct), "Cell Text"));

        // 4. 자동 재생 금지
        long autoplayCount = scan.getVideoAutoplayResults() != null ? scan.getVideoAutoplayResults().size() : 0;
        long autoplayIssues = scan.getVideoAutoplayResults() != null ? scan.getVideoAutoplayResults().stream()
                .filter(r -> !r.isCompliant()).count() : 0;
        double autoplayPct = autoplayCount > 0 ? ((autoplayCount - autoplayIssues) / (double) autoplayCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(4, "자동 재생 금지", "이슈 내용", round(autoplayPct), "Cell Text"));

        // 5. 텍스트 콘텐츠의 명도 대비
        long contrastCount = scan.getContrastResults() != null ? scan.getContrastResults().size() : 0;
        long contrastIssues = scan.getContrastResults() != null ? scan.getContrastResults().stream()
                .filter(r -> !r.isWcagCompliant()).count() : 0;
        double contrastPct = contrastCount > 0 ? ((contrastCount - contrastIssues) / (double) contrastCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(5, "텍스트 콘텐츠의 명도 대비", "Cell Text", round(contrastPct), "Cell Text"));

        // 6. 키보드 사용 보장
        long keyboardCount = scan.getKeyboardResults() != null ? scan.getKeyboardResults().size() : 0;
        long keyboardIssues = scan.getKeyboardResults() != null ? scan.getKeyboardResults().stream()
                .filter(r -> !r.isAccessible()).count() : 0;
        double keyboardPct = keyboardCount > 0 ? ((keyboardCount - keyboardIssues) / (double) keyboardCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(6, "키보드 사용 보장", "Cell Text", round(keyboardPct), "Cell Text"));

        // 7. 레이블 제공
        long labelCount = scan.getLabelResults() != null ? scan.getLabelResults().size() : 0;
        long labelIssues = scan.getLabelResults() != null ? scan.getLabelResults().stream()
                .filter(r -> !r.isLabelPresent()).count() : 0;
        double labelPct = labelCount > 0 ? ((labelCount - labelIssues) / (double) labelCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(7, "레이블 제공", "Cell Text", round(labelPct), "Cell Text"));

        // 8. 마크업 오류 방지
        long markupCount = scan.getMarkupErrorResults() != null ? scan.getMarkupErrorResults().size() : 0;
        long markupIssues = scan.getMarkupErrorResults() != null ? scan.getMarkupErrorResults().stream()
                .filter(r -> !r.isCompliant()).count() : 0;
        double markupPct = markupCount > 0 ? ((markupCount - markupIssues) / (double) markupCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(8, "마크업 오류 방지", "Cell Text", round(markupPct), "Cell Text"));

        // 9. 기본 언어 표시
        long langCount = scan.getBasicLanguageResults() != null ? scan.getBasicLanguageResults().size() : 0;
        long langIssues = scan.getBasicLanguageResults() != null ? scan.getBasicLanguageResults().stream()
                .filter(r -> !r.isCompliant()).count() : 0;
        double langPct = langCount > 0 ? ((langCount - langIssues) / (double) langCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(9, "기본 언어 표시", "Cell Text", round(langPct), "Cell Text"));

        // 10. 제목 제공
        long headingCount = scan.getHeadingResults() != null ? scan.getHeadingResults().size() : 0;
        int headingIssues = scan.getHeadingResults() != null ? scan.getHeadingResults().stream()
                .mapToInt(HeadingResult::getTotalIssues).sum() : 0;
        long headingCompliant = scan.getHeadingResults() != null ? scan.getHeadingResults().stream()
                .filter(HeadingResult::isCompliant).count() : 0;
        double headingPct = headingCount > 0 ? (headingCompliant / (double) headingCount * 100.0) : 100.0;
        String headingRate = headingIssues > 0 ? headingIssues + "개 문제" : "문제 없음";
        summary.add(new ScanCategorySummary(10, "제목 제공", headingRate, round(headingPct), headingIssues > 0 ? "오류 확인" : "준수"));

        // 11. 응답시간 조절
        long responseCount = scan.getResponseTimeResults() != null ? scan.getResponseTimeResults().size() : 0;
        long responseIssues = scan.getResponseTimeResults() != null ? scan.getResponseTimeResults().stream()
                .filter(r -> !r.isCompliant()).count() : 0;
        double responsePct = responseCount > 0 ? ((responseCount - responseIssues) / (double) responseCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(11, "응답시간 조절", "Cell Text", round(responsePct), "Cell Text"));

        // 12. 정지 기능 제공
        long pauseCount = scan.getPauseControlResults() != null ? scan.getPauseControlResults().size() : 0;
        long pauseIssues = scan.getPauseControlResults() != null ? scan.getPauseControlResults().stream()
                .filter(r -> !r.isCompliant()).count() : 0;
        double pausePct = pauseCount > 0 ? ((pauseCount - pauseIssues) / (double) pauseCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(12, "정지 기능 제공", "Cell Text", round(pausePct), "Cell Text"));

        // 13. 깜빡임과 번쩍임 사용 제한
        long flashCount = scan.getFlashingResults() != null ? scan.getFlashingResults().size() : 0;
        long flashIssues = scan.getFlashingResults() != null ? scan.getFlashingResults().stream()
                .filter(r -> !r.isCompliant()).count() : 0;
        double flashPct = flashCount > 0 ? ((flashCount - flashIssues) / (double) flashCount * 100.0) : 100.0;
        summary.add(new ScanCategorySummary(13, "깜빡임과 번쩍임 사용 제한", "Cell Text", round(flashPct), "Cell Text"));

        return summary;
    }

    private static double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
