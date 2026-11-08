package com.webridge.scan.checker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ContrastCalculatorTest {

    @Test
    @DisplayName("Black text on white background should have contrast 21:1")
    void blackOnWhiteContrast() {
        double ratio = ContrastCalculator.calculateContrastRatio("rgb(0, 0, 0)", "rgb(255, 255, 255)");
        assertThat(ratio).isCloseTo(21.0, within(0.01));
    }

    @Test
    @DisplayName("White text on black background should have contrast 21:1")
    void whiteOnBlackContrast() {
        double ratio = ContrastCalculator.calculateContrastRatio("rgb(255, 255, 255)", "rgb(0, 0, 0)");
        assertThat(ratio).isCloseTo(21.0, within(0.01));
    }

    @Test
    @DisplayName("Same color on same background should have contrast 1:1")
    void sameColorContrast() {
        double ratio = ContrastCalculator.calculateContrastRatio("rgb(100, 100, 100)", "rgb(100, 100, 100)");
        assertThat(ratio).isCloseTo(1.0, within(0.01));
    }

    @Test
    @DisplayName("Parse RGB and RGBA formats")
    void parseRgbFormats() {
        int[] rgb = ContrastCalculator.parseRgb("rgba(34, 139, 34, 0.8)");
        assertThat(rgb).containsExactly(34, 139, 34);

        int[] invalid = ContrastCalculator.parseRgb("invalid");
        assertThat(invalid).containsExactly(0, 0, 0);

        int[] empty = ContrastCalculator.parseRgb("");
        assertThat(empty).containsExactly(0, 0, 0);
    }

    @Test
    @DisplayName("Calculate contrast for typical grey text on white")
    void greyOnWhite() {
        // rgb(118, 118, 118) is the standard WCAG AA 4.5:1 boundary on white
        double ratio = ContrastCalculator.calculateContrastRatio("rgb(118, 118, 118)", "rgb(255, 255, 255)");
        assertThat(ratio).isGreaterThanOrEqualTo(4.5);
    }
}
