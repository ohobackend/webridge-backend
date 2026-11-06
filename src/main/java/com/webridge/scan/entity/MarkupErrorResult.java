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
@Table(name = "markup_error_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkupErrorResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private AccessibilityScan scan;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "error_detail", length = 500)
    private String errorDetail;

    @Column(name = "element_id", length = 200)
    private String elementId;

    @Column(name = "element_html", columnDefinition = "text")
    private String elementHtml;

    @Column(nullable = false)
    private boolean compliant;

    @Column(length = 200, nullable = false)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
