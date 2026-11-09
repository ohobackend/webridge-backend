package com.webridge.scan.report;

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
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class ExcelReportGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void generate(AccessibilityScan scan, File outputFile) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             OutputStream os = new FileOutputStream(outputFile)) {

            // Styles
            XSSFCellStyle titleStyle = wb.createCellStyle();
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFillForegroundColor(new XSSFColor(new Color(54, 96, 146), null));
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle headerStyle = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(new XSSFColor(new Color(54, 96, 146), null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(headerStyle);

            XSSFCellStyle boldKeyStyle = wb.createCellStyle();
            XSSFFont boldFont = wb.createFont();
            boldFont.setBold(true);
            boldKeyStyle.setFont(boldFont);
            boldKeyStyle.setFillForegroundColor(new XSSFColor(new Color(240, 240, 240), null));
            boldKeyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(boldKeyStyle);

            XSSFCellStyle dataStyle = wb.createCellStyle();
            setBorders(dataStyle);

            XSSFCellStyle centerDataStyle = wb.createCellStyle();
            centerDataStyle.setAlignment(HorizontalAlignment.CENTER);
            setBorders(centerDataStyle);

            // 1. Summary Sheet
            XSSFSheet summarySheet = wb.createSheet("검사 요약");

            // Title Banner
            Row r0 = summarySheet.createRow(0);
            r0.setHeightInPoints(40);
            for (int c = 0; c < 5; c++) {
                r0.createCell(c).setCellStyle(titleStyle);
            }
            r0.getCell(0).setCellValue("웹 접근성 검사 리포트");
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            // Info rows
            String[][] info = {
                    {"검사 URL", scan.getUrl() != null ? scan.getUrl() : ""},
                    {"검사 일시", scan.getCreatedAt() != null ? scan.getCreatedAt().format(DATE_FMT) : "N/A"},
                    {"완료 일시", scan.getCompletedAt() != null ? scan.getCompletedAt().format(DATE_FMT) : "N/A"},
                    {"총 문제 수", String.valueOf(scan.getTotalIssues())},
                    {"준수율", String.format("%.1f%%", scan.getComplianceScore())}
            };

            for (int i = 0; i < info.length; i++) {
                Row r = summarySheet.createRow(2 + i);
                var c0 = r.createCell(0);
                c0.setCellValue(info[i][0]);
                c0.setCellStyle(boldKeyStyle);

                var c1 = r.createCell(1);
                c1.setCellValue(info[i][1]);
                c1.setCellStyle(dataStyle);
            }

            // Summary Table Header
            int tableStartRow = 9;
            Row tableHead = summarySheet.createRow(tableStartRow);
            String[] headers = {"순위", "검사 항목", "총 요소 수", "문제 수", "준수율"};
            for (int c = 0; c < headers.length; c++) {
                var cell = tableHead.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(headerStyle);
            }

            // Summary Table Rows
            List<ScanCategorySummary> summaries = ScanDetailDto.buildScanSummary(scan);
            for (int i = 0; i < summaries.size(); i++) {
                ScanCategorySummary s = summaries.get(i);
                Row row = summarySheet.createRow(tableStartRow + 1 + i);

                var c0 = row.createCell(0);
                c0.setCellValue(s.rank());
                c0.setCellStyle(centerDataStyle);

                var c1 = row.createCell(1);
                c1.setCellValue(s.category());
                c1.setCellStyle(dataStyle);

                var c2 = row.createCell(2);
                c2.setCellValue(parseTotalCount(s.complianceRate()));
                c2.setCellStyle(centerDataStyle);

                var c3 = row.createCell(3);
                c3.setCellValue(parseIssueCount(s.complianceRate()));
                c3.setCellStyle(centerDataStyle);

                var c4 = row.createCell(4);
                c4.setCellValue(String.format("%.1f%%", s.compliancePercentage()));
                c4.setCellStyle(centerDataStyle);
            }

            for (int c = 0; c < 5; c++) {
                summarySheet.autoSizeColumn(c);
            }

            // 2. Detail Sheets
            addAltTextSheet(wb, scan, headerStyle, dataStyle, centerDataStyle);
            addContrastSheet(wb, scan, headerStyle, dataStyle, centerDataStyle);
            addKeyboardSheet(wb, scan, headerStyle, dataStyle, centerDataStyle);
            addLabelSheet(wb, scan, headerStyle, dataStyle, centerDataStyle);
            addTableSheet(wb, scan, headerStyle, dataStyle, centerDataStyle);
            addVideoSheet(wb, scan, headerStyle, dataStyle, centerDataStyle);

            wb.write(os);
        }
    }

    private void addAltTextSheet(XSSFWorkbook wb, AccessibilityScan scan, XSSFCellStyle headerStyle, XSSFCellStyle dataStyle, XSSFCellStyle centerStyle) {
        if (scan.getAltTextResults() == null || scan.getAltTextResults().isEmpty()) return;
        XSSFSheet sheet = wb.createSheet("이미지 대체 텍스트");
        Row h = sheet.createRow(0);
        String[] headers = {"이미지 URL", "기존 Alt 텍스트", "제안 Alt 텍스트", "준수 상태"};
        for (int i = 0; i < headers.length; i++) {
            var cell = h.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < scan.getAltTextResults().size(); i++) {
            AltTextResult r = scan.getAltTextResults().get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.getImgUrl());
            row.createCell(1).setCellValue(r.getAltText() != null ? r.getAltText() : "N/A");
            row.createCell(2).setCellValue(r.getSuggestedAlt() != null ? r.getSuggestedAlt() : "");
            row.createCell(3).setCellValue(mapComplianceStatus(r.getCompliance()));

            for (int c = 0; c < 4; c++) {
                row.getCell(c).setCellStyle(c == 3 ? centerStyle : dataStyle);
            }
        }
        for (int c = 0; c < 4; c++) sheet.autoSizeColumn(c);
    }

    private void addContrastSheet(XSSFWorkbook wb, AccessibilityScan scan, XSSFCellStyle headerStyle, XSSFCellStyle dataStyle, XSSFCellStyle centerStyle) {
        if (scan.getContrastResults() == null || scan.getContrastResults().isEmpty()) return;
        XSSFSheet sheet = wb.createSheet("색상 대비");
        Row h = sheet.createRow(0);
        String[] headers = {"텍스트 내용", "전경색", "배경색", "대비율", "WCAG 준수"};
        for (int i = 0; i < headers.length; i++) {
            var cell = h.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < scan.getContrastResults().size(); i++) {
            ContrastResult r = scan.getContrastResults().get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.getTextContent());
            row.createCell(1).setCellValue(r.getForegroundColor() != null ? r.getForegroundColor() : "");
            row.createCell(2).setCellValue(r.getBackgroundColor() != null ? r.getBackgroundColor() : "");
            row.createCell(3).setCellValue(r.getContrastRatio());
            row.createCell(4).setCellValue(r.isWcagCompliant() ? "준수" : "미준수");

            for (int c = 0; c < 5; c++) {
                row.getCell(c).setCellStyle(c >= 1 ? centerStyle : dataStyle);
            }
        }
        for (int c = 0; c < 5; c++) sheet.autoSizeColumn(c);
    }

    private void addKeyboardSheet(XSSFWorkbook wb, AccessibilityScan scan, XSSFCellStyle headerStyle, XSSFCellStyle dataStyle, XSSFCellStyle centerStyle) {
        if (scan.getKeyboardResults() == null || scan.getKeyboardResults().isEmpty()) return;
        XSSFSheet sheet = wb.createSheet("키보드 접근성");
        Row h = sheet.createRow(0);
        String[] headers = {"요소 타입", "접근 가능 여부", "메시지"};
        for (int i = 0; i < headers.length; i++) {
            var cell = h.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < scan.getKeyboardResults().size(); i++) {
            KeyboardResult r = scan.getKeyboardResults().get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.getElementType() != null ? r.getElementType() : "");
            row.createCell(1).setCellValue(r.isAccessible() ? "접근 가능" : "접근 불가");
            row.createCell(2).setCellValue(r.getMessage() != null ? r.getMessage() : "");

            for (int c = 0; c < 3; c++) {
                row.getCell(c).setCellStyle(c == 1 ? centerStyle : dataStyle);
            }
        }
        for (int c = 0; c < 3; c++) sheet.autoSizeColumn(c);
    }

    private void addLabelSheet(XSSFWorkbook wb, AccessibilityScan scan, XSSFCellStyle headerStyle, XSSFCellStyle dataStyle, XSSFCellStyle centerStyle) {
        if (scan.getLabelResults() == null || scan.getLabelResults().isEmpty()) return;
        XSSFSheet sheet = wb.createSheet("레이블");
        Row h = sheet.createRow(0);
        String[] headers = {"입력 타입", "레이블 존재 여부", "메시지"};
        for (int i = 0; i < headers.length; i++) {
            var cell = h.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < scan.getLabelResults().size(); i++) {
            LabelResult r = scan.getLabelResults().get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.getInputType() != null ? r.getInputType() : "");
            row.createCell(1).setCellValue(r.isLabelPresent() ? "존재" : "미제공");
            row.createCell(2).setCellValue(r.getMessage() != null ? r.getMessage() : "");

            for (int c = 0; c < 3; c++) {
                row.getCell(c).setCellStyle(c == 1 ? centerStyle : dataStyle);
            }
        }
        for (int c = 0; c < 3; c++) sheet.autoSizeColumn(c);
    }

    private void addTableSheet(XSSFWorkbook wb, AccessibilityScan scan, XSSFCellStyle headerStyle, XSSFCellStyle dataStyle, XSSFCellStyle centerStyle) {
        if (scan.getTableResults() == null || scan.getTableResults().isEmpty()) return;
        XSSFSheet sheet = wb.createSheet("표 구조");
        Row h = sheet.createRow(0);
        String[] headers = {"헤더 수", "행 수", "준수 여부", "메시지"};
        for (int i = 0; i < headers.length; i++) {
            var cell = h.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < scan.getTableResults().size(); i++) {
            TableResult r = scan.getTableResults().get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.getHeadersCount());
            row.createCell(1).setCellValue(r.getRowsCount());
            row.createCell(2).setCellValue(r.isCompliant() ? "준수" : "미준수");
            row.createCell(3).setCellValue(r.getMessage() != null ? r.getMessage() : "");

            for (int c = 0; c < 4; c++) {
                row.getCell(c).setCellStyle(c < 3 ? centerStyle : dataStyle);
            }
        }
        for (int c = 0; c < 4; c++) sheet.autoSizeColumn(c);
    }

    private void addVideoSheet(XSSFWorkbook wb, AccessibilityScan scan, XSSFCellStyle headerStyle, XSSFCellStyle dataStyle, XSSFCellStyle centerStyle) {
        if (scan.getVideoCaptionResults() == null || scan.getVideoCaptionResults().isEmpty()) return;
        XSSFSheet sheet = wb.createSheet("동영상 접근성");
        Row h = sheet.createRow(0);
        String[] headers = {"동영상 URL", "캡션 유무", "설명"};
        for (int i = 0; i < headers.length; i++) {
            var cell = h.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < scan.getVideoCaptionResults().size(); i++) {
            VideoCaptionResult r = scan.getVideoCaptionResults().get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.getVideoUrl());
            row.createCell(1).setCellValue(r.isHasTranscript() ? "있음" : "없음");
            row.createCell(2).setCellValue(r.getMessage() != null ? r.getMessage() : "");

            for (int c = 0; c < 3; c++) {
                row.getCell(c).setCellStyle(c == 1 ? centerStyle : dataStyle);
            }
        }
        for (int c = 0; c < 3; c++) sheet.autoSizeColumn(c);
    }

    private void setBorders(XSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String mapComplianceStatus(int c) {
        return switch (c) {
            case 0 -> "정상";
            case 1 -> "개선필요";
            case 2 -> "미준수";
            default -> "오류";
        };
    }

    private String parseTotalCount(String rate) {
        if (rate == null) return "0";
        if (rate.contains("/")) {
            String[] parts = rate.split("/");
            return parts.length > 1 ? parts[1].trim() : "0";
        }
        return rate;
    }

    private String parseIssueCount(String rate) {
        if (rate == null) return "0";
        if (rate.contains("/")) {
            return rate.split("/")[0].trim();
        }
        if (rate.contains("개")) {
            return rate.replaceAll("[^0-9]", "");
        }
        return "0";
    }
}
