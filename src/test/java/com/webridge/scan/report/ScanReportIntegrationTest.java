package com.webridge.scan.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.auth.jwt.JwtProvider;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.report.dto.ScanReportRequest;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.scan.repository.ScanReportRepository;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("schematest")
class ScanReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AccessibilityScanRepository scanRepo;

    @Autowired
    private ScanReportRepository reportRepo;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ScanReportService reportService;

    @MockBean
    private ScanReportTaskRunner taskRunner;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() {
        doNothing().when(taskRunner).runReportGeneration(any(), any());

        reportRepo.deleteAll();
        scanRepo.deleteAll();
        userRepo.deleteAll();

        testUser = userRepo.save(User.builder()
                .email("report-user@example.com")
                .name("Report User")
                .password("hash")
                .isActive(true)
                .build());
        token = jwtProvider.createAccess(testUser.getId());
    }

    @Test
    @DisplayName("POST /api/v1/scans/{scanId}/generate-report/ - Success for completed scan")
    void generateReportSuccess() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.COMPLETED)
                .progress(100)
                .totalIssues(3)
                .complianceScore(92.0)
                .build());

        ScanReportRequest req = new ScanReportRequest("pdf");

        mockMvc.perform(post("/api/v1/scans/" + scan.getId() + "/generate-report/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("리포트 생성이 시작되었습니다."))
                .andExpect(jsonPath("$.task_id").isNotEmpty())
                .andExpect(jsonPath("$.format").value("pdf"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/{scanId}/generate-report/ - Processing scan returns 400")
    void generateReportProcessingFails() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.PROCESSING)
                .progress(50)
                .build());

        ScanReportRequest req = new ScanReportRequest("pdf");

        mockMvc.perform(post("/api/v1/scans/" + scan.getId() + "/generate-report/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("완료된 검사만 리포트를 생성할 수 있습니다."));
    }

    @Test
    @DisplayName("POST /api/v1/scans/{scanId}/generate-report/ - Unsupported format returns 400")
    void generateReportInvalidFormat() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.COMPLETED)
                .build());

        ScanReportRequest req = new ScanReportRequest("docx");

        mockMvc.perform(post("/api/v1/scans/" + scan.getId() + "/generate-report/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("지원하지 않는 리포트 형식입니다. (pdf, excel, json만 지원)"));
    }

    @Test
    @DisplayName("GET /api/v1/scans/{scanId}/download-report/ - Not found if report not generated")
    void downloadReportNotFound() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.COMPLETED)
                .build());

        mockMvc.perform(get("/api/v1/scans/" + scan.getId() + "/download-report/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("생성된 리포트가 없습니다. 먼저 리포트를 생성해주세요."));
    }

    @Test
    @DisplayName("GET /api/v1/scans/{scanId}/download-report/ - Downloads file and increments count")
    void downloadReportSuccess() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.COMPLETED)
                .progress(100)
                .complianceScore(95.0)
                .build());

        // Synchronously generate a JSON report file
        File file = reportService.generateReportFile(scan.getId(), "json");
        assertThat(file).exists();

        mockMvc.perform(get("/api/v1/scans/" + scan.getId() + "/download-report/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"accessibility_report_" + scan.getId() + ".json\""))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        var report = reportRepo.findByScanId(scan.getId()).orElseThrow();
        assertThat(report.getDownloadCount()).isEqualTo(1);

        // Clean up generated file
        Files.deleteIfExists(file.toPath());
    }
}
