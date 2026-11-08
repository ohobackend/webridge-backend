package com.webridge.scan.checker;

import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.BasicLanguageResult;
import com.webridge.scan.entity.ContrastResult;
import com.webridge.scan.entity.FlashingResult;
import com.webridge.scan.entity.HeadingResult;
import com.webridge.scan.entity.KeyboardResult;
import com.webridge.scan.entity.LabelResult;
import com.webridge.scan.entity.MarkupErrorResult;
import com.webridge.scan.entity.PauseControlResult;
import com.webridge.scan.entity.ResponseTimeResult;
import com.webridge.scan.entity.TableResult;
import com.webridge.scan.entity.VideoAutoplayResult;
import com.webridge.scan.entity.VideoCaptionResult;

public final class ScoreCalculator {

    private ScoreCalculator() {}

    public static int calculateTotalIssues(AccessibilityScan scan) {
        int total = 0;

        // 1. alt_text (compliance in [1, 2])
        if (scan.getAltTextResults() != null) {
            total += scan.getAltTextResults().stream()
                    .filter(r -> r.getCompliance() == 1 || r.getCompliance() == 2)
                    .count();
        }

        // 2. contrast (wcag_compliant == false)
        if (scan.getContrastResults() != null) {
            total += scan.getContrastResults().stream()
                    .filter(r -> !r.isWcagCompliant())
                    .count();
        }

        // 3. keyboard (accessible == false)
        if (scan.getKeyboardResults() != null) {
            total += scan.getKeyboardResults().stream()
                    .filter(r -> !r.isAccessible())
                    .count();
        }

        // 4. label (label_present == false)
        if (scan.getLabelResults() != null) {
            total += scan.getLabelResults().stream()
                    .filter(r -> !r.isLabelPresent())
                    .count();
        }

        // 5. table (compliant == false)
        if (scan.getTableResults() != null) {
            total += scan.getTableResults().stream()
                    .filter(r -> !r.isCompliant())
                    .count();
        }

        // 6. video caption (compliant == false)
        if (scan.getVideoCaptionResults() != null) {
            total += scan.getVideoCaptionResults().stream()
                    .filter(r -> !r.isCompliant())
                    .count();
        }

        // 7. video autoplay (compliant == false)
        if (scan.getVideoAutoplayResults() != null) {
            total += scan.getVideoAutoplayResults().stream()
                    .filter(r -> !r.isCompliant())
                    .count();
        }

        // 8. basic language (compliant == false)
        if (scan.getBasicLanguageResults() != null) {
            total += scan.getBasicLanguageResults().stream()
                    .filter(r -> !r.isCompliant())
                    .count();
        }

        // 9. markup error (compliant == false)
        if (scan.getMarkupErrorResults() != null) {
            total += scan.getMarkupErrorResults().stream()
                    .filter(r -> !r.isCompliant())
                    .count();
        }

        // 10. headings (each non-compliant HeadingResult uses totalIssues)
        if (scan.getHeadingResults() != null) {
            for (HeadingResult hr : scan.getHeadingResults()) {
                if (!hr.isCompliant()) {
                    total += hr.getTotalIssues();
                }
            }
        }

        // 11. response time (compliant == false)
        if (scan.getResponseTimeResults() != null) {
            total += scan.getResponseTimeResults().stream()
                    .filter(r -> !r.isCompliant())
                    .count();
        }

        // 12. pause control (compliant == false)
        if (scan.getPauseControlResults() != null) {
            total += scan.getPauseControlResults().stream()
                    .filter(r -> !r.isCompliant())
                    .count();
        }

        // 13. flashing (compliant == false)
        if (scan.getFlashingResults() != null) {
            total += scan.getFlashingResults().stream()
                    .filter(r -> !r.isCompliant())
                    .count();
        }

        return total;
    }

    public static double calculateComplianceScore(AccessibilityScan scan) {
        long totalElements = 0;
        long compliantElements = 0;

        // 1. alt_text (compliance == 0)
        if (scan.getAltTextResults() != null) {
            totalElements += scan.getAltTextResults().size();
            compliantElements += scan.getAltTextResults().stream()
                    .filter(r -> r.getCompliance() == 0)
                    .count();
        }

        // 2. contrast (wcagCompliant == true)
        if (scan.getContrastResults() != null) {
            totalElements += scan.getContrastResults().size();
            compliantElements += scan.getContrastResults().stream()
                    .filter(ContrastResult::isWcagCompliant)
                    .count();
        }

        // 3. keyboard (accessible == true)
        if (scan.getKeyboardResults() != null) {
            totalElements += scan.getKeyboardResults().size();
            compliantElements += scan.getKeyboardResults().stream()
                    .filter(KeyboardResult::isAccessible)
                    .count();
        }

        // 4. label (labelPresent == true)
        if (scan.getLabelResults() != null) {
            totalElements += scan.getLabelResults().size();
            compliantElements += scan.getLabelResults().stream()
                    .filter(LabelResult::isLabelPresent)
                    .count();
        }

        // 5. table (compliant == true)
        if (scan.getTableResults() != null) {
            totalElements += scan.getTableResults().size();
            compliantElements += scan.getTableResults().stream()
                    .filter(TableResult::isCompliant)
                    .count();
        }

        // 6. video caption (compliant == true)
        if (scan.getVideoCaptionResults() != null) {
            totalElements += scan.getVideoCaptionResults().size();
            compliantElements += scan.getVideoCaptionResults().stream()
                    .filter(VideoCaptionResult::isCompliant)
                    .count();
        }

        // 7. video autoplay (compliant == true)
        if (scan.getVideoAutoplayResults() != null) {
            totalElements += scan.getVideoAutoplayResults().size();
            compliantElements += scan.getVideoAutoplayResults().stream()
                    .filter(VideoAutoplayResult::isCompliant)
                    .count();
        }

        // 8. basic language (compliant == true)
        if (scan.getBasicLanguageResults() != null) {
            totalElements += scan.getBasicLanguageResults().size();
            compliantElements += scan.getBasicLanguageResults().stream()
                    .filter(BasicLanguageResult::isCompliant)
                    .count();
        }

        // 9. markup error (compliant == true)
        if (scan.getMarkupErrorResults() != null) {
            totalElements += scan.getMarkupErrorResults().size();
            compliantElements += scan.getMarkupErrorResults().stream()
                    .filter(MarkupErrorResult::isCompliant)
                    .count();
        }

        // 10. headings (compliant == true)
        if (scan.getHeadingResults() != null) {
            totalElements += scan.getHeadingResults().size();
            compliantElements += scan.getHeadingResults().stream()
                    .filter(HeadingResult::isCompliant)
                    .count();
        }

        // 11. response time (compliant == true)
        if (scan.getResponseTimeResults() != null) {
            totalElements += scan.getResponseTimeResults().size();
            compliantElements += scan.getResponseTimeResults().stream()
                    .filter(ResponseTimeResult::isCompliant)
                    .count();
        }

        // 12. pause control (compliant == true)
        if (scan.getPauseControlResults() != null) {
            totalElements += scan.getPauseControlResults().size();
            compliantElements += scan.getPauseControlResults().stream()
                    .filter(PauseControlResult::isCompliant)
                    .count();
        }

        // 13. flashing (compliant == true)
        if (scan.getFlashingResults() != null) {
            totalElements += scan.getFlashingResults().size();
            compliantElements += scan.getFlashingResults().stream()
                    .filter(FlashingResult::isCompliant)
                    .count();
        }

        if (totalElements == 0) {
            return 100.0;
        }

        return ((double) compliantElements / (double) totalElements) * 100.0;
    }
}
