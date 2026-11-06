package com.webridge.scan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "scan_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", unique = true, nullable = false)
    private AccessibilityScan scan;

    @Builder.Default
    @Column(length = 10, nullable = false)
    private String format = "pdf";

    @Column(name = "file_path", length = 100)
    private String filePath;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private OffsetDateTime generatedAt;

    @Builder.Default
    @Column(name = "download_count", nullable = false)
    private int downloadCount = 0;
}
