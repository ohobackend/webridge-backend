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

    @BeforeAll
    static void setUp() throws IOException {
        scripts = new CheckerScripts();
        File accessibleFile = new ClassPathResource("fixtures/accessible_page.html").getFile();
        File inaccessibleFile = new ClassPathResource("fixtures/inaccessible_page.html").getFile();
        accessibleUrl = accessibleFile.toURI().toString();
        inaccessibleUrl = inaccessibleFile.toURI().toString();
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
        }
    }
}
