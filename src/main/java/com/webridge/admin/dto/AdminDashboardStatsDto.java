package com.webridge.admin.dto;

import java.util.List;

public record AdminDashboardStatsDto(
    // 사용자 통계
    long totalUsers,
    long todayNewUsers,
    long activeUsers,
    long adminUsers,
    // 검사 통계
    long totalScans,
    long todayScans,
    long completedScans,
    long failedScans,
    double averageComplianceScore,
    // 일별 추이 (최근 7일)
    List<AdminDailyStatDto> dailyNewUsers,
    List<AdminDailyStatDto> dailyScans,
    // 최근 내역 (각 5건)
    List<AdminScanSummaryDto> recentScans,
    List<AdminUserSummaryDto> recentUsers
) {}
