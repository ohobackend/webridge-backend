package com.webridge.scan.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.AltTextResult;
import com.webridge.scan.entity.ContrastResult;
import com.webridge.scan.entity.KeyboardResult;
import com.webridge.scan.entity.LabelResult;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.entity.TableResult;
import com.webridge.scan.entity.VideoCaptionResult;
import com.webridge.user.entity.User;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScanReportGeneratorTest {

    private PdfReportGenerator pdfGenerator;
    private ExcelReportGenerator excelGenerator;
    private JsonReportGenerator jsonGenerator;

    private AccessibilityScan sampleScan;

    @BeforeEach
    void setUp() {
        pdfGenerator = new PdfReportGenerator();
        excelGenerator = new ExcelReportGenerator();
        jsonGenerator = new JsonReportGenerator(new ObjectMapper());

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트 사용자")
                .build();

        sampleScan = AccessibilityScan.builder()
                .id(UUID.randomUUID())
                .user(user)
                .url("https://example.com")
                .title("테스트 웹사이트")
                .status(ScanStatus.COMPLETED)
                .progress(100)
                .totalIssues(5)
                .complianceScore(85.5)
                .altTextCompleted(true)
                .contrastCompleted(true)
                .keyboardCompleted(true)
                .labelCompleted(true)
                .tableCompleted(true)
                .videoCaptionCompleted(true)
                .createdAt(OffsetDateTime.now().minusHours(1))
                .completedAt(OffsetDateTime.now())
                .build();

        // Add dummy results
        sampleScan.getAltTextResults().add(AltTextResult.builder()
                .scan(sampleScan)
                .imgUrl("https://example.com/logo.png")
                .altText("로고")
                .suggestedAlt("위브릿지 로고")
                .compliance(0)
                .build());

        sampleScan.getContrastResults().add(ContrastResult.builder()
                .scan(sampleScan)
                .textContent("안녕하세요")
                .foregroundColor("#000000")
                .backgroundColor("#ffffff")
                .contrastRatio(21.0)
                .wcagCompliant(true)
                .build());

        sampleScan.getKeyboardResults().add(KeyboardResult.builder()
                .scan(sampleScan)
                .elementType("button")
                .accessible(true)
                .message("초점 이동 가능")
                .build());

        sampleScan.getLabelResults().add(LabelResult.builder()
                .scan(sampleScan)
                .inputType("text")
                .labelPresent(true)
                .message("레이블 정상")
                .build());

        sampleScan.getTableResults().add(TableResult.builder()
                .scan(sampleScan)
                .headersCount(3)
                .rowsCount(5)
                .compliant(true)
                .message("th 태그 포함")
                .build());

        sampleScan.getVideoCaptionResults().add(VideoCaptionResult.builder()
                .scan(sampleScan)
                .videoUrl("https://example.com/video.mp4")
                .hasTranscript(true)
                .message("자막 제공")
                .build());
    }

    @Test
    @DisplayName("PDF Report Generator generates a valid PDF file with Korean characters")
    void generatePdf(@TempDir Path tempDir) throws Exception {
        File pdfFile = tempDir.resolve("report.pdf").toFile();

        pdfGenerator.generate(sampleScan, pdfFile);

        assertThat(pdfFile).exists();
        assertThat(pdfFile.length()).isGreaterThan(1000);
    }

    @Test
    @DisplayName("Excel Report Generator generates a valid XLSX file with summary and detail sheets")
    void generateExcel(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("report.xlsx").toFile();

        excelGenerator.generate(sampleScan, excelFile);

        assertThat(excelFile).exists();
        assertThat(excelFile.length()).isGreaterThan(1000);

        try (var fis = new FileInputStream(excelFile);
             var wb = new XSSFWorkbook(fis)) {
            assertThat(wb.getNumberOfSheets()).isGreaterThanOrEqualTo(2);
            assertThat(wb.getSheet("검사 요약")).isNotNull();
            assertThat(wb.getSheet("이미지 대체 텍스트")).isNotNull();
            assertThat(wb.getSheet("검사 요약").getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("웹 접근성 검사 리포트");
        }
    }

    @Test
    @DisplayName("JSON Report Generator generates a valid JSON file with scan_info and results")
    void generateJson(@TempDir Path tempDir) throws Exception {
        File jsonFile = tempDir.resolve("report.json").toFile();

        jsonGenerator.generate(sampleScan, jsonFile);

        assertThat(jsonFile).exists();
        assertThat(jsonFile.length()).isGreaterThan(100);

        ObjectMapper om = new ObjectMapper();
        var node = om.readTree(jsonFile);
        assertThat(node.has("scan_info")).isTrue();
        assertThat(node.get("scan_info").get("url").asText()).isEqualTo("https://example.com");
        assertThat(node.has("results")).isTrue();
        assertThat(node.get("results").get("alt_text").isArray()).isTrue();
    }
}
