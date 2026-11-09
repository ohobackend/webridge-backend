package com.webridge.scan.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.webridge.scan.dto.ScanCategorySummary;
import com.webridge.scan.dto.ScanDetailDto;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.AltTextResult;
import com.webridge.scan.entity.ContrastResult;
import com.webridge.scan.entity.KeyboardResult;
import com.webridge.scan.entity.LabelResult;
import com.webridge.scan.entity.TableResult;
import com.webridge.scan.entity.VideoCaptionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class PdfReportGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm");

    public void generate(AccessibilityScan scan, File outputFile) throws Exception {
        try (OutputStream os = new FileOutputStream(outputFile)) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, os);
            doc.open();

            // 1. Title
            var title = new Paragraph("웹 접근성 검사 리포트", PdfFontHelper.bold(22));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            doc.add(title);

            // 2. Info Section
            var infoHead = new Paragraph("검사 정보", PdfFontHelper.bold(14));
            infoHead.setSpacingAfter(10);
            doc.add(infoHead);

            PdfPTable infoTable = new PdfPTable(new float[]{1.5f, 4.5f});
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20);

            addInfoRow(infoTable, "검사 URL", scan.getUrl() != null ? scan.getUrl() : "");
            addInfoRow(infoTable, "검사 일시", scan.getCreatedAt() != null ? scan.getCreatedAt().format(DATE_FMT) : "N/A");
            addInfoRow(infoTable, "완료 일시", scan.getCompletedAt() != null ? scan.getCompletedAt().format(DATE_FMT) : "N/A");
            addInfoRow(infoTable, "총 문제 수", String.valueOf(scan.getTotalIssues()));
            addInfoRow(infoTable, "준수율", String.format("%.1f%%", scan.getComplianceScore()));
            doc.add(infoTable);

            // 3. Summary Section
            var sumHead = new Paragraph("검사 결과 요약", PdfFontHelper.bold(14));
            sumHead.setSpacingAfter(10);
            doc.add(sumHead);

            PdfPTable sumTable = new PdfPTable(new float[]{0.8f, 3.2f, 1.2f, 1.2f});
            sumTable.setWidthPercentage(100);
            sumTable.setSpacingAfter(25);

            Color headerBg = new Color(41, 74, 110);
            Color headerText = Color.WHITE;
            addHeaderCell(sumTable, "순위", headerBg, headerText);
            addHeaderCell(sumTable, "검사 항목", headerBg, headerText);
            addHeaderCell(sumTable, "문제 수", headerBg, headerText);
            addHeaderCell(sumTable, "준수율", headerBg, headerText);

            List<ScanCategorySummary> summaries = ScanDetailDto.buildScanSummary(scan);
            for (ScanCategorySummary s : summaries) {
                addDataCell(sumTable, String.valueOf(s.rank()), Element.ALIGN_CENTER);
                addDataCell(sumTable, s.category(), Element.ALIGN_LEFT);
                addDataCell(sumTable, s.complianceRate(), Element.ALIGN_CENTER);
                addDataCell(sumTable, String.format("%.1f%%", s.compliancePercentage()), Element.ALIGN_CENTER);
            }
            doc.add(sumTable);

            // 4. Detailed Results
            addDetailedResults(doc, scan);

            doc.close();
        }
    }

    private void addDetailedResults(Document doc, AccessibilityScan scan) throws Exception {
        Color detailHeaderBg = new Color(220, 230, 242);
        Color detailHeaderText = new Color(30, 30, 30);

        // 4.1 Alt Text
        if (scan.getAltTextResults() != null && !scan.getAltTextResults().isEmpty()) {
            var head = new Paragraph("이미지 대체 텍스트 상세 결과", PdfFontHelper.bold(12));
            head.setSpacingAfter(8);
            doc.add(head);

            PdfPTable table = new PdfPTable(new float[]{2.5f, 1.5f, 2.0f, 0.8f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            addHeaderCell(table, "이미지 URL", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "기존 Alt", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "제안 Alt", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "상태", detailHeaderBg, detailHeaderText);

            int limit = Math.min(10, scan.getAltTextResults().size());
            for (int i = 0; i < limit; i++) {
                AltTextResult r = scan.getAltTextResults().get(i);
                addDataCell(table, truncate(r.getImgUrl(), 45), Element.ALIGN_LEFT);
                addDataCell(table, truncate(r.getAltText() != null ? r.getAltText() : "N/A", 25), Element.ALIGN_LEFT);
                addDataCell(table, truncate(r.getSuggestedAlt() != null ? r.getSuggestedAlt() : "", 35), Element.ALIGN_LEFT);
                addDataCell(table, mapComplianceStatus(r.getCompliance()), Element.ALIGN_CENTER);
            }
            doc.add(table);
        }

        // 4.2 Contrast
        if (scan.getContrastResults() != null && !scan.getContrastResults().isEmpty()) {
            var head = new Paragraph("색상 대비 상세 결과", PdfFontHelper.bold(12));
            head.setSpacingAfter(8);
            doc.add(head);

            PdfPTable table = new PdfPTable(new float[]{2.5f, 1.2f, 1.2f, 0.9f, 0.9f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            addHeaderCell(table, "텍스트", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "전경색", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "배경색", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "대비율", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "WCAG 준수", detailHeaderBg, detailHeaderText);

            int limit = Math.min(10, scan.getContrastResults().size());
            for (int i = 0; i < limit; i++) {
                ContrastResult r = scan.getContrastResults().get(i);
                addDataCell(table, truncate(r.getTextContent(), 30), Element.ALIGN_LEFT);
                addDataCell(table, r.getForegroundColor() != null ? r.getForegroundColor() : "", Element.ALIGN_CENTER);
                addDataCell(table, r.getBackgroundColor() != null ? r.getBackgroundColor() : "", Element.ALIGN_CENTER);
                addDataCell(table, String.format("%.2f", r.getContrastRatio()), Element.ALIGN_CENTER);
                addDataCell(table, r.isWcagCompliant() ? "준수" : "미준수", Element.ALIGN_CENTER);
            }
            doc.add(table);
        }

        // 4.3 Keyboard
        if (scan.getKeyboardResults() != null && !scan.getKeyboardResults().isEmpty()) {
            var head = new Paragraph("키보드 접근성 상세 결과", PdfFontHelper.bold(12));
            head.setSpacingAfter(8);
            doc.add(head);

            PdfPTable table = new PdfPTable(new float[]{2.0f, 1.2f, 3.5f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            addHeaderCell(table, "요소 타입", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "접근 가능 여부", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "메시지", detailHeaderBg, detailHeaderText);

            int limit = Math.min(10, scan.getKeyboardResults().size());
            for (int i = 0; i < limit; i++) {
                KeyboardResult r = scan.getKeyboardResults().get(i);
                addDataCell(table, r.getElementType() != null ? r.getElementType() : "", Element.ALIGN_LEFT);
                addDataCell(table, r.isAccessible() ? "접근 가능" : "접근 불가", Element.ALIGN_CENTER);
                addDataCell(table, r.getMessage() != null ? r.getMessage() : "", Element.ALIGN_LEFT);
            }
            doc.add(table);
        }

        // 4.4 Label
        if (scan.getLabelResults() != null && !scan.getLabelResults().isEmpty()) {
            var head = new Paragraph("레이블 상세 결과", PdfFontHelper.bold(12));
            head.setSpacingAfter(8);
            doc.add(head);

            PdfPTable table = new PdfPTable(new float[]{2.0f, 1.2f, 3.5f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            addHeaderCell(table, "입력 타입", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "레이블 존재", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "메시지", detailHeaderBg, detailHeaderText);

            int limit = Math.min(10, scan.getLabelResults().size());
            for (int i = 0; i < limit; i++) {
                LabelResult r = scan.getLabelResults().get(i);
                addDataCell(table, r.getInputType() != null ? r.getInputType() : "", Element.ALIGN_LEFT);
                addDataCell(table, r.isLabelPresent() ? "존재" : "미제공", Element.ALIGN_CENTER);
                addDataCell(table, r.getMessage() != null ? r.getMessage() : "", Element.ALIGN_LEFT);
            }
            doc.add(table);
        }

        // 4.5 Table Structure
        if (scan.getTableResults() != null && !scan.getTableResults().isEmpty()) {
            var head = new Paragraph("표 구조 상세 결과", PdfFontHelper.bold(12));
            head.setSpacingAfter(8);
            doc.add(head);

            PdfPTable table = new PdfPTable(new float[]{1.0f, 1.0f, 1.2f, 3.5f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            addHeaderCell(table, "헤더 수", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "행 수", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "준수 여부", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "메시지", detailHeaderBg, detailHeaderText);

            int limit = Math.min(10, scan.getTableResults().size());
            for (int i = 0; i < limit; i++) {
                TableResult r = scan.getTableResults().get(i);
                addDataCell(table, String.valueOf(r.getHeadersCount()), Element.ALIGN_CENTER);
                addDataCell(table, String.valueOf(r.getRowsCount()), Element.ALIGN_CENTER);
                addDataCell(table, r.isCompliant() ? "준수" : "미준수", Element.ALIGN_CENTER);
                addDataCell(table, r.getMessage() != null ? r.getMessage() : "", Element.ALIGN_LEFT);
            }
            doc.add(table);
        }

        // 4.6 Video Caption
        if (scan.getVideoCaptionResults() != null && !scan.getVideoCaptionResults().isEmpty()) {
            var head = new Paragraph("동영상 자막 상세 결과", PdfFontHelper.bold(12));
            head.setSpacingAfter(8);
            doc.add(head);

            PdfPTable table = new PdfPTable(new float[]{3.0f, 1.2f, 2.5f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            addHeaderCell(table, "동영상 URL", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "캡션 유무", detailHeaderBg, detailHeaderText);
            addHeaderCell(table, "설명", detailHeaderBg, detailHeaderText);

            int limit = Math.min(10, scan.getVideoCaptionResults().size());
            for (int i = 0; i < limit; i++) {
                VideoCaptionResult r = scan.getVideoCaptionResults().get(i);
                addDataCell(table, truncate(r.getVideoUrl(), 40), Element.ALIGN_LEFT);
                addDataCell(table, r.isHasTranscript() ? "있음" : "없음", Element.ALIGN_CENTER);
                addDataCell(table, r.getMessage() != null ? r.getMessage() : "", Element.ALIGN_LEFT);
            }
            doc.add(table);
        }
    }

    private void addInfoRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key, PdfFontHelper.bold(10)));
        keyCell.setBackgroundColor(new Color(240, 240, 240));
        keyCell.setPadding(6);
        keyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(keyCell);

        PdfPCell valCell = new PdfPCell(new Phrase(value, PdfFontHelper.regular(10)));
        valCell.setPadding(6);
        valCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(valCell);
    }

    private void addHeaderCell(PdfPTable table, String text, Color bg, Color textColor) {
        var font = PdfFontHelper.bold(10);
        font.setColor(textColor);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", PdfFontHelper.regular(9)));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String mapComplianceStatus(int c) {
        return switch (c) {
            case 0 -> "정상";
            case 1 -> "개선필요";
            case 2 -> "미준수";
            default -> "오류";
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
