package com.webridge.scan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alt_text_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AltTextResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private AccessibilityScan scan;

    @Column(name = "img_url", columnDefinition = "text", nullable = false)
    private String imgUrl;

    @Builder.Default
    @Column(name = "element_html", columnDefinition = "text", nullable = false)
    private String elementHtml = "";

    @Column(name = "alt_text", columnDefinition = "text")
    private String altText;

    @Builder.Default
    @Column(name = "suggested_alt", columnDefinition = "text", nullable = false)
    private String suggestedAlt = "";

    @Column(nullable = false)
    private int compliance;

    @Builder.Default
    @Column(name = "ai_type", length = 50, nullable = false)
    private String aiType = "";

    @Builder.Default
    @Column(name = "ai_grade", length = 20, nullable = false)
    private String aiGrade = "";

    @Builder.Default
    @Column(name = "ai_reason", columnDefinition = "text", nullable = false)
    private String aiReason = "";

    @Builder.Default
    @Column(name = "ai_improvement", columnDefinition = "text", nullable = false)
    private String aiImprovement = "";

    @Builder.Default
    @Column(name = "has_error", nullable = false)
    private boolean hasError = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
