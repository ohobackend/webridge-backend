package com.webridge.scan.scheduler;

import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("schematest")
@Transactional
class ScanCleanupServiceTest {

    @Autowired
    private ScanCleanupService cleanupService;

    @Autowired
    private AccessibilityScanRepository scanRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("Stuck scan older than 30 minutes is marked as failed and reset")
    void cleanupStuckScan() {
        User user = userRepo.save(User.builder()
                .email("cleanup-test@example.com")
                .name("Cleanup User")
                .password("hash")
                .isActive(true)
                .build());

        AccessibilityScan scan = AccessibilityScan.builder()
                .user(user)
                .url("https://example.com")
                .status(ScanStatus.PROCESSING)
                .progress(45)
                .altTextCompleted(true)
                .build();
        scanRepo.saveAndFlush(scan);

        // Manually adjust updatedAt to 40 minutes ago via native SQL to bypass @UpdateTimestamp
        jdbcTemplate.update("UPDATE accessibility_scans SET updated_at = ? WHERE id = ?",
                OffsetDateTime.now().minusMinutes(40), scan.getId());
        em.clear();

        int cleaned = cleanupService.cleanup(30);
        assertThat(cleaned).isGreaterThanOrEqualTo(1);

        AccessibilityScan cleanedScan = scanRepo.findById(scan.getId()).orElseThrow();
        assertThat(cleanedScan.getStatus()).isEqualTo(ScanStatus.FAILED);
        assertThat(cleanedScan.getProgress()).isEqualTo(0);
        assertThat(cleanedScan.getErrorMessage()).contains("타임아웃");
        assertThat(cleanedScan.isAltTextCompleted()).isFalse();
    }

    @Test
    @DisplayName("Active scan within timeout is not cleaned up")
    void ignoreActiveRecentScan() {
        User user = userRepo.save(User.builder()
                .email("recent-test@example.com")
                .name("Recent User")
                .password("hash")
                .isActive(true)
                .build());

        AccessibilityScan scan = AccessibilityScan.builder()
                .user(user)
                .url("https://example.com")
                .status(ScanStatus.PROCESSING)
                .progress(20)
                .build();
        scanRepo.save(scan);
        scan.setUpdatedAt(OffsetDateTime.now().minusMinutes(5));
        scanRepo.save(scan);

        int cleaned = cleanupService.cleanup(30);
        assertThat(cleaned).isEqualTo(0);

        AccessibilityScan unchanged = scanRepo.findById(scan.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ScanStatus.PROCESSING);
    }
}
