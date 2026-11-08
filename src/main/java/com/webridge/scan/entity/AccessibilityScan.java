package com.webridge.scan.entity;

import com.webridge.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accessibility_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessibilityScan {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "text", nullable = false)
    private String url;

    @Builder.Default
    @Column(length = 200, nullable = false)
    private String title = "";

    @Builder.Default
    @Convert(converter = ScanStatusConverter.class)
    @Column(length = 20, nullable = false)
    private ScanStatus status = ScanStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private int progress = 0;

    @Builder.Default
    @Column(name = "total_issues", nullable = false)
    private int totalIssues = 0;

    @Builder.Default
    @Column(name = "compliance_score", nullable = false)
    private double complianceScore = 0.0;

    @Column(name = "alt_text_completed", nullable = false)
    private boolean altTextCompleted;

    @Column(name = "contrast_completed", nullable = false)
    private boolean contrastCompleted;

    @Column(name = "keyboard_completed", nullable = false)
    private boolean keyboardCompleted;

    @Column(name = "label_completed", nullable = false)
    private boolean labelCompleted;

    @Column(name = "table_completed", nullable = false)
    private boolean tableCompleted;

    @Column(name = "video_caption_completed", nullable = false)
    private boolean videoCaptionCompleted;

    @Column(name = "video_autoplay_completed", nullable = false)
    private boolean videoAutoplayCompleted;

    @Column(name = "basic_language_completed", nullable = false)
    private boolean basicLanguageCompleted;

    @Column(name = "markup_error_completed", nullable = false)
    private boolean markupErrorCompleted;

    @Column(name = "heading_completed", nullable = false)
    private boolean headingCompleted;

    @Column(name = "response_time_completed", nullable = false)
    private boolean responseTimeCompleted;

    @Column(name = "pause_control_completed", nullable = false)
    private boolean pauseControlCompleted;

    @Column(name = "flashing_completed", nullable = false)
    private boolean flashingCompleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<AltTextResult> altTextResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<ContrastResult> contrastResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<KeyboardResult> keyboardResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<LabelResult> labelResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<TableResult> tableResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<VideoCaptionResult> videoCaptionResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<VideoAutoplayResult> videoAutoplayResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<BasicLanguageResult> basicLanguageResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<MarkupErrorResult> markupErrorResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<HeadingResult> headingResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<ResponseTimeResult> responseTimeResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<PauseControlResult> pauseControlResults = new java.util.ArrayList<>();

    @Builder.Default
    @jakarta.persistence.OneToMany(mappedBy = "scan", cascade = jakarta.persistence.CascadeType.REMOVE)
    private java.util.List<FlashingResult> flashingResults = new java.util.ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public boolean isCompleted() {
        return status == ScanStatus.COMPLETED;
    }

    public double getCompletionPercentage() {
        int count = 0;
        if (altTextCompleted) count++;
        if (contrastCompleted) count++;
        if (keyboardCompleted) count++;
        if (labelCompleted) count++;
        if (tableCompleted) count++;
        if (videoCaptionCompleted) count++;
        if (videoAutoplayCompleted) count++;
        if (basicLanguageCompleted) count++;
        if (markupErrorCompleted) count++;
        if (headingCompleted) count++;
        if (responseTimeCompleted) count++;
        if (pauseControlCompleted) count++;
        if (flashingCompleted) count++;
        return (count / 13.0) * 100.0;
    }

    public void resetForRun() {
        this.status = ScanStatus.PROCESSING;
        this.progress = 0;
        this.errorMessage = null;
        this.totalIssues = 0;
        this.complianceScore = 0.0;
        this.completedAt = null;
        this.altTextCompleted = false;
        this.contrastCompleted = false;
        this.keyboardCompleted = false;
        this.labelCompleted = false;
        this.tableCompleted = false;
        this.videoCaptionCompleted = false;
        this.videoAutoplayCompleted = false;
        this.basicLanguageCompleted = false;
        this.markupErrorCompleted = false;
        this.headingCompleted = false;
        this.responseTimeCompleted = false;
        this.pauseControlCompleted = false;
        this.flashingCompleted = false;
    }
}
