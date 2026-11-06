package com.webridge.survey.entity;

import com.webridge.user.entity.User;
import jakarta.persistence.Column;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "surveys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Survey {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 254, nullable = false)
    private String email;

    @Builder.Default
    @Column(name = "privacy_agreed", nullable = false)
    private boolean privacyAgreed = false;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "company_type", columnDefinition = "jsonb", nullable = false)
    private List<String> companyType = new ArrayList<>();

    @Builder.Default
    @Column(name = "company_type_other", length = 500, nullable = false)
    private String companyTypeOther = "";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "usage_reason", columnDefinition = "jsonb", nullable = false)
    private List<String> usageReason = new ArrayList<>();

    @Builder.Default
    @Column(name = "usage_reason_other", length = 500, nullable = false)
    private String usageReasonOther = "";

    @Column(name = "satisfaction_overall", nullable = false)
    private int satisfactionOverall;

    @Column(name = "satisfaction_accuracy", nullable = false)
    private int satisfactionAccuracy;

    @Column(name = "satisfaction_reuse", nullable = false)
    private int satisfactionReuse;

    @Column(name = "satisfaction_recommend", nullable = false)
    private int satisfactionRecommend;

    @Column(name = "user_type", length = 1, nullable = false)
    private String userType;

    @Builder.Default
    @Column(name = "user_type_other", length = 500, nullable = false)
    private String userTypeOther = "";

    @Column(name = "purchase_method", length = 1, nullable = false)
    private String purchaseMethod;

    @Builder.Default
    @Column(name = "purchase_method_other", length = 500, nullable = false)
    private String purchaseMethodOther = "";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "usage_method", columnDefinition = "jsonb", nullable = false)
    private List<String> usageMethod = new ArrayList<>();

    @Builder.Default
    @Column(name = "usage_method_other", length = 500, nullable = false)
    private String usageMethodOther = "";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "future_feature", columnDefinition = "jsonb", nullable = false)
    private List<String> futureFeature = new ArrayList<>();

    @Builder.Default
    @Column(name = "future_feature_other", length = 500, nullable = false)
    private String futureFeatureOther = "";

    @Builder.Default
    @Column(name = "improvement_opinion", columnDefinition = "text", nullable = false)
    private String improvementOpinion = "";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
