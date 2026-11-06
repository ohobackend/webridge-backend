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
@Table(name = "video_autoplay_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAutoplayResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private AccessibilityScan scan;

    @Builder.Default
    @Column(name = "video_url", columnDefinition = "text", nullable = false)
    private String videoUrl = "";

    @Builder.Default
    @Column(name = "element_html", columnDefinition = "text", nullable = false)
    private String elementHtml = "";

    @Builder.Default
    @Column(name = "autoplay_disabled", nullable = false)
    private boolean autoplayDisabled = true;

    @Builder.Default
    @Column(name = "keyboard_accessible", nullable = false)
    private boolean keyboardAccessible = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean compliant = true;

    @Builder.Default
    @Column(length = 200, nullable = false)
    private String message = "";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
