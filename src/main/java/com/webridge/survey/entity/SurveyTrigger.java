package com.webridge.survey.entity;

import com.webridge.user.entity.User;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "survey_triggers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "completed_scans_count", nullable = false)
    private int completedScansCount = 0;

    @Builder.Default
    @Column(name = "has_issues_found", nullable = false)
    private boolean hasIssuesFound = false;

    @Builder.Default
    @Column(name = "survey_shown", nullable = false)
    private boolean surveyShown = false;

    @Builder.Default
    @Column(name = "survey_completed", nullable = false)
    private boolean surveyCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public boolean shouldShowSurvey() {
        return completedScansCount >= 1 && hasIssuesFound && !surveyShown && !surveyCompleted;
    }
}
