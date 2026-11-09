package com.webridge.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.auth.jwt.JwtProvider;
import com.webridge.scan.dto.ScanCreateRequest;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.scan.runner.ScanTaskRunner;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("schematest")
class ScanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AccessibilityScanRepository scanRepo;

    @Autowired
    private JwtProvider jwtProvider;

    @MockBean
    private ScanTaskRunner taskRunner;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() {
        doNothing().when(taskRunner).run(any());

        testUser = userRepo.save(User.builder()
                .email("scan-user-" + UUID.randomUUID() + "@example.com")
                .name("Scan Tester")
                .password("hash")
                .isActive(true)
                .build());
        token = jwtProvider.createAccess(testUser.getId());
    }

    @Test
    @DisplayName("POST /api/v1/scans/create/ - Success")
    void createScanSuccess() throws Exception {
        ScanCreateRequest req = new ScanCreateRequest("https://example.com");

        mockMvc.perform(post("/api/v1/scans/create/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("웹 접근성 검사가 시작되었습니다."))
                .andExpect(jsonPath("$.task_id").isNotEmpty())
                .andExpect(jsonPath("$.scan.url").value("https://example.com"))
                .andExpect(jsonPath("$.scan.status").value("pending"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/create/ - Invalid URL returns 400")
    void createScanInvalidUrl() throws Exception {
        ScanCreateRequest req = new ScanCreateRequest("ftp://example.com");

        mockMvc.perform(post("/api/v1/scans/create/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.url[0]").value("올바른 URL 형식이 아닙니다. http:// 또는 https://로 시작해야 합니다."));
    }

    @Test
    @DisplayName("GET /api/v1/scans/ - List with pagination and filter")
    void listScans() throws Exception {
        scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://site1.com")
                .status(ScanStatus.COMPLETED)
                .progress(100)
                .build());

        scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://site2.com")
                .status(ScanStatus.FAILED)
                .progress(0)
                .build());

        mockMvc.perform(get("/api/v1/scans/?status=completed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.results[0].url").value("https://site1.com"))
                .andExpect(jsonPath("$.results[0].status").value("completed"));
    }

    @Test
    @DisplayName("GET /api/v1/scans/{scanId}/ - Detail with summary")
    void detailScan() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.COMPLETED)
                .progress(100)
                .totalIssues(3)
                .complianceScore(85.5)
                .build());

        mockMvc.perform(get("/api/v1/scans/" + scan.getId() + "/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scan.getId().toString()))
                .andExpect(jsonPath("$.url").value("https://example.com"))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.total_issues").value(3))
                .andExpect(jsonPath("$.scan_summary").isArray())
                .andExpect(jsonPath("$.scan_summary.length()").value(13));
    }

    @Test
    @DisplayName("GET /api/v1/scans/{scanId}/progress/ - Progress inquiry")
    void progressScan() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.PROCESSING)
                .progress(45)
                .altTextCompleted(true)
                .build());

        mockMvc.perform(get("/api/v1/scans/" + scan.getId() + "/progress/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scan_id").value(scan.getId().toString()))
                .andExpect(jsonPath("$.status").value("processing"))
                .andExpect(jsonPath("$.progress").value(45))
                .andExpect(jsonPath("$.current_test").value("색상 대비 검사"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/{scanId}/restart/ - Restart scan")
    void restartScan() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.FAILED)
                .progress(20)
                .build());

        mockMvc.perform(post("/api/v1/scans/" + scan.getId() + "/restart/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("검사가 다시 시작되었습니다."))
                .andExpect(jsonPath("$.scan.status").value("pending"));
    }

    @Test
    @DisplayName("DELETE /api/v1/scans/{scanId}/delete/ - Delete scan")
    void deleteScan() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.COMPLETED)
                .build());

        mockMvc.perform(delete("/api/v1/scans/" + scan.getId() + "/delete/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(scanRepo.findById(scan.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/scans/{scanId}/delete/ - Processing scan cannot be deleted")
    void deleteProcessingScanFails() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.PROCESSING)
                .build());

        mockMvc.perform(delete("/api/v1/scans/" + scan.getId() + "/delete/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("진행 중인 검사는 삭제할 수 없습니다."));
    }

    @Test
    @DisplayName("GET /api/v1/scans/{scanId}/stream/ - SSE Stream produces event")
    void sseStream() throws Exception {
        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(testUser)
                .url("https://example.com")
                .status(ScanStatus.COMPLETED)
                .progress(100)
                .build());

        MvcResult result = mockMvc.perform(get("/api/v1/scans/" + scan.getId() + "/stream/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("data:");
        assertThat(responseBody).contains("\"scan_id\":\"" + scan.getId() + "\"");
    }
}
