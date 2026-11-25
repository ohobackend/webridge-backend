package com.webridge.scan.checker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccessibilityCheckerTest {

    private static CheckerScripts scripts;
    private static String accessibleUrl;
    private static String inaccessibleUrl;
    private static String pauseControlUrl;

    @BeforeAll
    static void setUp() throws IOException {
        scripts = new CheckerScripts();
        File accessibleFile = new ClassPathResource("fixtures/accessible_page.html").getFile();
        File inaccessibleFile = new ClassPathResource("fixtures/inaccessible_page.html").getFile();
        File pauseControlFile = new ClassPathResource("fixtures/pause_control_page.html").getFile();
        accessibleUrl = accessibleFile.toURI().toString();
        inaccessibleUrl = inaccessibleFile.toURI().toString();
        pauseControlUrl = pauseControlFile.toURI().toString();
    }

    @Test
    @DisplayName("Verify accessible fixture page passes all standard accessibility checks")
    void checkAccessiblePage() {
        try (AccessibilityChecker checker = new AccessibilityChecker(accessibleUrl, scripts, null)) {
            // Page Title
            String title = checker.extractPageTitle();
            assertThat(title).isEqualTo("웹 접근성 테스트 페이지 (정상)");

            // 1. Basic Language
            var langResults = checker.checkBasicLanguage();
            assertThat(langResults).hasSize(1);
            assertThat(langResults.get(0).get("compliant")).isEqualTo(true);
            assertThat(langResults.get(0).get("lang_attribute")).isEqualTo("ko");

            // 2. Labels
            var labelResults = checker.checkLabels();
            assertThat(labelResults).isNotEmpty();
            assertThat(labelResults).allMatch(r -> Boolean.TRUE.equals(r.get("label_present")));

            // 3. Tables
            var tableResults = checker.checkTables();
            assertThat(tableResults).hasSize(1);
            assertThat(tableResults.get(0).get("compliant")).isEqualTo(true);
            assertThat((String) tableResults.get(0).get("screenshot_url")).startsWith("data:image/jpeg;base64,");

            // 4. Keyboard
            var keyboardResults = checker.checkKeyboard();
            assertThat(keyboardResults).isNotEmpty();
            assertThat(keyboardResults).allMatch(r -> Boolean.TRUE.equals(r.get("accessible")));

            // 5. Markup errors
            var markupResults = checker.checkMarkupErrors();
            assertThat(markupResults).isEmpty();

            // 6. Video captions & autoplay
            var captionResults = checker.checkVideoCaptions();
            assertThat(captionResults).hasSize(1);
            assertThat(captionResults.get(0).get("compliant")).isEqualTo(true);

            var autoplayResults = checker.checkVideoAutoplay();
            assertThat(autoplayResults).hasSize(1);
            assertThat(autoplayResults.get(0).get("compliant")).isEqualTo(true);

            // 7. Alt text
            var altResults = checker.checkAltText();
            assertThat(altResults).isNotEmpty();
            assertThat(altResults.get(0).get("compliant")).isEqualTo(0); // SVG is 0 (compliant)
        }
    }

    @Test
    @DisplayName("Verify inaccessible fixture page detects multiple violations correctly")
    void checkInaccessiblePage() {
        try (AccessibilityChecker checker = new AccessibilityChecker(inaccessibleUrl, scripts, null)) {
            // 1. Basic Language: missing lang
            var langResults = checker.checkBasicLanguage();
            assertThat(langResults).hasSize(1);
            assertThat(langResults.get(0).get("compliant")).isEqualTo(false);

            // 2. Labels: unlabeled-input
            var labelResults = checker.checkLabels();
            assertThat(labelResults).anyMatch(r -> Boolean.FALSE.equals(r.get("label_present")));

            // 3. Markup errors: duplicate ID, img without src, empty h2/a
            var markupResults = checker.checkMarkupErrors();
            assertThat(markupResults).isNotEmpty();
            assertThat(markupResults).anyMatch(r -> "중복 ID".equals(r.get("error_type")));
            assertThat(markupResults).anyMatch(r -> "필수 속성 누락".equals(r.get("error_type")));
            assertThat(markupResults).anyMatch(r -> "빈 요소".equals(r.get("error_type")));

            // 4. Headings: title too short, empty headings, section without heading, special symbols
            var headingResults = checker.checkHeadings();
            assertThat(headingResults).hasSize(1);
            assertThat(headingResults.get(0).get("compliant")).isEqualTo(false);

            // 5. Video Autoplay: autoplay enabled
            var autoplayResults = checker.checkVideoAutoplay();
            assertThat(autoplayResults).hasSize(1);
            assertThat(autoplayResults.get(0).get("compliant")).isEqualTo(false);

            // 6. Flashing: fast animation < 333ms
            var flashingResults = checker.checkFlashing();
            assertThat(flashingResults).isNotEmpty();
            assertThat(flashingResults).anyMatch(r -> Boolean.FALSE.equals(r.get("compliant")));

            // 7. Contrast: low-contrast text
            var contrastResults = checker.checkContrast();
            assertThat(contrastResults).anyMatch(r -> Boolean.FALSE.equals(r.get("wcag_compliant")));
            assertThat(contrastResults).anyMatch(r -> {
                String url = (String) r.get("screenshot_url");
                return url != null && url.startsWith("data:image/jpeg;base64,");
            });
        }
    }

    @Test
    @DisplayName("Verify pause control captures DOM screenshots for non-img containers and excludes pagination bullets")
    void checkPauseControlCapturesScreenshotsAndExcludesPaginationBullets() {
        try (AccessibilityChecker checker = new AccessibilityChecker(pauseControlUrl, scripts, null)) {
            List<Map<String, Object>> results = checker.checkPauseControl(3);

            assertThat(results).isNotEmpty();
            // pagination bullet 요소는 검사 결과에서 제외되어야 함
            assertThat(results).noneMatch(r -> {
                String tag = String.valueOf(r.get("tag"));
                String html = String.valueOf(r.get("element_html"));
                return tag.contains("pagination") || tag.contains("bullet") || html.contains("flicking-pagination-bullet");
            });
            // 모든 수집 항목에 실제 DOM 캡처 이미지(data:image/jpeg;base64,...)가 포함되어야 함
            assertThat(results).allSatisfy(r -> {
                String screenshotUrl = (String) r.get("screenshot_url");
                String captureUrl = (String) r.get("capture_url");
                assertThat(screenshotUrl).isNotNull().startsWith("data:image/jpeg;base64,");
                assertThat(captureUrl).isEqualTo(screenshotUrl);
            });
            // 텍스트 배너(#promo-banner)와 캐러셀 컨테이너(.flicking-viewport)가 컨테이너 단위로 수집되어야 함
            assertThat(results).anyMatch(r -> String.valueOf(r.get("tag")).contains("promo-banner"));
            assertThat(results).anyMatch(r -> String.valueOf(r.get("tag")).contains("flicking-viewport"));
        }
    }
}
