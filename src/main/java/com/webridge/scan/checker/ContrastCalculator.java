package com.webridge.scan.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ContrastCalculator {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private ContrastCalculator() {}

    public static double calculateContrastRatio(String fgColor, String bgColor) {
        int[] fg = parseRgb(fgColor);
        int[] bg = parseRgb(bgColor);
        double l1 = luminance(fg) + 0.05;
        double l2 = luminance(bg) + 0.05;
        return Math.max(l1, l2) / Math.min(l1, l2);
    }

    public static int[] parseRgb(String color) {
        if (color == null || color.isBlank()) {
            return new int[]{0, 0, 0};
        }
        Matcher matcher = NUMBER_PATTERN.matcher(color);
        List<Integer> nums = new ArrayList<>(3);
        while (matcher.find()) {
            try {
                nums.add(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException ignored) {}
        }
        if (nums.size() >= 3) {
            return new int[]{nums.get(0), nums.get(1), nums.get(2)};
        }
        return new int[]{0, 0, 0};
    }

    public static double luminance(int[] rgb) {
        double r = toLinear(rgb[0]);
        double g = toLinear(rgb[1]);
        double b = toLinear(rgb[2]);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double toLinear(int value) {
        double v = value / 255.0;
        return (v <= 0.03928) ? (v / 12.92) : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
