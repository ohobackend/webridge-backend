package com.webridge.admin.dto;

import java.util.List;

public record AdminSignupSurveyStatsDto(
    long totalRespondedUsers,
    long totalUsers,
    double responseRate,
    List<CategoryStatDto> pathStats,
    List<CategoryStatDto> occupationStats,
    List<String> recentPathOtherAnswers,
    List<String> recentOccupationOtherAnswers
) {
    public record CategoryStatDto(
        String code,
        String label,
        long count,
        double percentage
    ) {}
}
