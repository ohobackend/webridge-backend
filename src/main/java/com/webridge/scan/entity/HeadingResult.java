package com.webridge.scan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "heading_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeadingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private AccessibilityScan scan;

    @Builder.Default
    @Column(name = "page_title_exists", nullable = false)
    private boolean pageTitleExists = false;

    @Builder.Default
    @Column(name = "page_title_empty", nullable = false)
    private boolean pageTitleEmpty = true;

    @Column(name = "page_title_text", length = 500)
    private String pageTitleText;

    @Builder.Default
    @Column(name = "page_title_length", nullable = false)
    private int pageTitleLength = 0;

    @Builder.Default
    @Column(name = "total_popups", nullable = false)
    private int totalPopups = 0;

    @Builder.Default
    @Column(name = "popups_without_title", nullable = false)
    private int popupsWithoutTitle = 0;

    @Builder.Default
    @Column(name = "total_frames", nullable = false)
    private int totalFrames = 0;

    @Builder.Default
    @Column(name = "frames_without_title", nullable = false)
    private int framesWithoutTitle = 0;

    @Builder.Default
    @Column(name = "total_headings", nullable = false)
    private int totalHeadings = 0;

    @Builder.Default
    @Column(name = "empty_headings", nullable = false)
    private int emptyHeadings = 0;

    @Builder.Default
    @Column(name = "headings_without_content", nullable = false)
    private int headingsWithoutContent = 0;

    @Builder.Default
    @Column(name = "total_semantic_sections")
    private int totalSemanticSections = 0;

    @Builder.Default
    @Column(name = "sections_without_headings", nullable = false)
    private int sectionsWithoutHeadings = 0;

    @Builder.Default
    @Column(name = "special_symbol_issues", nullable = false)
    private int specialSymbolIssues = 0;

    @Builder.Default
    @Column(name = "total_issues", nullable = false)
    private int totalIssues = 0;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issues_details", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> issuesDetails = new ArrayList<>();

    @Builder.Default
    @Column(name = "element_html", columnDefinition = "text")
    private String elementHtml = "";

    @Column(nullable = false)
    private boolean compliant;

    @Column(length = 200, nullable = false)
    private String message;

    @Builder.Default
    @Column(name = "compliance_rate", length = 50)
    private String complianceRate = "100%";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detailed_results", columnDefinition = "jsonb")
    private Map<String, Object> detailedResults = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    @PreUpdate
    public void calculateTotalIssues() {
        this.totalIssues = (
            (!pageTitleExists || pageTitleEmpty ? 1 : 0) +
            popupsWithoutTitle +
            framesWithoutTitle +
            emptyHeadings +
            headingsWithoutContent +
            sectionsWithoutHeadings +
            specialSymbolIssues
        );
    }

    public boolean isPageTitleCompliant() {
        return pageTitleExists && !pageTitleEmpty && pageTitleLength >= 3;
    }

    public boolean isPopupFramesCompliant() {
        return popupsWithoutTitle == 0 && framesWithoutTitle == 0;
    }

    public boolean isContentHeadingsCompliant() {
        return emptyHeadings == 0 && headingsWithoutContent == 0 && sectionsWithoutHeadings == 0;
    }

    public boolean isSpecialSymbolsCompliant() {
        return specialSymbolIssues == 0;
    }
}
