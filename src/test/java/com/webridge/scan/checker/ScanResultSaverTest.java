package com.webridge.scan.checker;

import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
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
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("schematest")
@Transactional
class ScanResultSaverTest {

    @Autowired
    private ScanResultSaver saver;

    @Autowired
    private AccessibilityScanRepository scanRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private BasicLanguageResultRepository basicLanguageRepo;

    @Autowired
    private ContrastResultRepository contrastRepo;

    @Test
    @DisplayName("ScanResultSaver correctly persists check results and calculates final score")
    void testSaveAndComplete() {
        User user = userRepo.save(User.builder()
                .email("scan-saver-test@example.com")
                .name("Saver User")
                .password("pbkdf2_sha256$260000$test$hash")
                .isActive(true)
                .build());

        AccessibilityScan scan = scanRepo.save(AccessibilityScan.builder()
                .user(user)
                .url("https://example.com")
                .build());

        saver.resetForRun(scan.getId());
        saver.saveTitleIfEmpty(scan.getId(), "Example Domain");

        // 1. Language: compliant
        saver.saveBasicLanguage(scan.getId(), List.of(Map.of(
                "lang_attribute", "ko",
                "compliant", true,
                "message", "기본 언어가 설정되었습니다."
        )));

        // 2. Contrast: 1 compliant, 1 non-compliant
        saver.saveContrast(scan.getId(), List.of(
                Map.of(
                        "text", "Hello",
                        "element_html", "<h1>Hello</h1>",
                        "foreground_color", "rgb(0, 0, 0)",
                        "background_color", "rgb(255, 255, 255)",
                        "contrast_ratio", 21.0,
                        "wcag_compliant", true
                ),
                Map.of(
                        "text", "Grey",
                        "element_html", "<p>Grey</p>",
                        "foreground_color", "rgb(200, 200, 200)",
                        "background_color", "rgb(255, 255, 255)",
                        "contrast_ratio", 1.8,
                        "wcag_compliant", false
                )
        ));

        // Complete
        saver.complete(scan.getId());

        AccessibilityScan completedScan = scanRepo.findById(scan.getId()).orElseThrow();
        assertThat(completedScan.getStatus()).isEqualTo(ScanStatus.COMPLETED);
        assertThat(completedScan.getProgress()).isEqualTo(100);
        assertThat(completedScan.getTitle()).isEqualTo("Example Domain");
        assertThat(completedScan.getTotalIssues()).isEqualTo(1); // 1 contrast non-compliant
        // 2 compliant out of 3 elements = 66.67%
        assertThat(completedScan.getComplianceScore()).isBetween(66.0, 67.0);
        assertThat(completedScan.getCompletedAt()).isNotNull();

        assertThat(basicLanguageRepo.findByScanId(scan.getId())).hasSize(1);
        assertThat(contrastRepo.findByScanId(scan.getId())).hasSize(2);
    }
}
