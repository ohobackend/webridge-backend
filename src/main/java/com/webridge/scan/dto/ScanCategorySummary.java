package com.webridge.scan.dto;

public record ScanCategorySummary(
        int rank,
        String category,
        String complianceRate,
        double compliancePercentage,
        String errorType
) {}
