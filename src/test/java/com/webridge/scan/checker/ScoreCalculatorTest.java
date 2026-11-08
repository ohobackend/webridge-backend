package com.webridge.scan.checker;

import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.AltTextResult;
import com.webridge.scan.entity.BasicLanguageResult;
import com.webridge.scan.entity.ContrastResult;
import com.webridge.scan.entity.HeadingResult;
import com.webridge.scan.entity.KeyboardResult;
import com.webridge.scan.entity.LabelResult;
import com.webridge.scan.entity.TableResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ScoreCalculatorTest {

    @Test
    @DisplayName("Empty scan results should return 0 issues and 100% compliance")
    void emptyScan() {
        AccessibilityScan scan = AccessibilityScan.builder().build();
        assertThat(ScoreCalculator.calculateTotalIssues(scan)).isEqualTo(0);
        assertThat(ScoreCalculator.calculateComplianceScore(scan)).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Scan with some compliant and non-compliant elements calculates score and issues accurately")
    void mixedResults() {
        AccessibilityScan scan = AccessibilityScan.builder()
                .altTextResults(List.of(
                        AltTextResult.builder().compliance(0).build(), // compliant
                        AltTextResult.builder().compliance(1).build(), // issue (partially compliant)
                        AltTextResult.builder().compliance(2).build()  // issue (non compliant)
                ))
                .contrastResults(List.of(
                        ContrastResult.builder().wcagCompliant(true).build(),
                        ContrastResult.builder().wcagCompliant(false).build() // issue
                ))
                .keyboardResults(List.of(
                        KeyboardResult.builder().accessible(true).build()
                ))
                .labelResults(List.of(
                        LabelResult.builder().labelPresent(false).build() // issue
                ))
                .tableResults(List.of(
                        TableResult.builder().compliant(true).build()
                ))
                .basicLanguageResults(List.of(
                        BasicLanguageResult.builder().compliant(true).build()
                ))
                .headingResults(List.of(
                        HeadingResult.builder()
                                .compliant(false)
                                .emptyHeadings(2)
                                .popupsWithoutTitle(1)
                                .build() // calculateTotalIssues in HeadingResult gives 2 + 1 + (pageTitleMissing ? 1 : 0)
                ))
                .build();

        // Trigger HeadingResult @PrePersist/@PreUpdate
        scan.getHeadingResults().get(0).calculateTotalIssues();

        int issues = ScoreCalculator.calculateTotalIssues(scan);
        // alt: 2 issues (compliance 1, 2)
        // contrast: 1 issue
        // label: 1 issue
        // heading: totalIssues = 2 (empty) + 1 (popups) + 1 (pageTitleExists=false) = 4
        // total issues = 2 + 1 + 1 + 4 = 8
        assertThat(issues).isEqualTo(8);

        // Compliance score:
        // totalElements: 3 (alt) + 2 (contrast) + 1 (keyboard) + 1 (label) + 1 (table) + 1 (lang) + 1 (heading) = 10
        // compliantElements: 1 (alt:0) + 1 (contrast) + 1 (keyboard) + 0 (label) + 1 (table) + 1 (lang) + 0 (heading) = 5
        // 5 / 10 = 50.0%
        double score = ScoreCalculator.calculateComplianceScore(scan);
        assertThat(score).isCloseTo(50.0, within(0.01));
    }
}
