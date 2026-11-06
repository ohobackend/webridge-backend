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
@Table(name = "label_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private AccessibilityScan scan;

    @Column(name = "input_type", length = 50, nullable = false)
    private String inputType;

    @Builder.Default
    @Column(name = "element_html", columnDefinition = "text", nullable = false)
    private String elementHtml = "";

    @Column(name = "label_present", nullable = false)
    private boolean labelPresent;

    @Column(length = 200, nullable = false)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
