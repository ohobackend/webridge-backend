package com.webridge.scan.dto;

import com.webridge.scan.entity.HeadingResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record HeadingItemDto(
        Long id,
        boolean pageTitleExists,
        boolean pageTitleEmpty,
        String pageTitleText,
        int pageTitleLength,
        boolean pageTitleCompliant,
        int totalPopups,
        int popupsWithoutTitle,
        boolean popupFramesCompliant,
        int totalFrames,
        int framesWithoutTitle,
        int totalHeadings,
        int emptyHeadings,
        int headingsWithoutContent,
        int totalSemanticSections,
        int sectionsWithoutHeadings,
        boolean contentHeadingsCompliant,
        int specialSymbolIssues,
        boolean specialSymbolsCompliant,
        int totalIssues,
        List<Map<String, Object>> issuesDetails,
        String elementHtml,
        boolean compliant,
        String message,
        Map<String, Object> detailedResults,
        OffsetDateTime createdAt
) {
    public static HeadingItemDto from(HeadingResult r) {
        boolean pageTitleCompliant = r.isPageTitleCompliant();
        boolean popupFramesCompliant = r.isPopupFramesCompliant();
        boolean contentHeadingsCompliant = r.isContentHeadingsCompliant();
        boolean specialSymbolsCompliant = r.isSpecialSymbolsCompliant();

        return new HeadingItemDto(
                r.getId(),
                r.isPageTitleExists(),
                r.isPageTitleEmpty(),
                r.getPageTitleText(),
                r.getPageTitleLength(),
                pageTitleCompliant,
                r.getTotalPopups(),
                r.getPopupsWithoutTitle(),
                popupFramesCompliant,
                r.getTotalFrames(),
                r.getFramesWithoutTitle(),
                r.getTotalHeadings(),
                r.getEmptyHeadings(),
                r.getHeadingsWithoutContent(),
                r.getTotalSemanticSections(),
                r.getSectionsWithoutHeadings(),
                contentHeadingsCompliant,
                r.getSpecialSymbolIssues(),
                specialSymbolsCompliant,
                r.getTotalIssues(),
                r.getIssuesDetails(),
                r.getElementHtml(),
                r.isCompliant(),
                r.getMessage(),
                r.getDetailedResults(),
                r.getCreatedAt()
        );
    }
}
