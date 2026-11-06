package com.webridge.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String password;

    private OffsetDateTime lastLogin;

    @Column(nullable = false)
    private boolean isSuperuser;

    @Column(length = 255, unique = true, nullable = false)
    private String email;

    @Column(length = 100, nullable = false)
    private String name;

    @Builder.Default
    @Column(length = 20, nullable = false)
    private String provider = "email";

    @Column(length = 255)
    private String providerId;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean isAdmin;

    @Column(nullable = false)
    private boolean serviceTermsAgreed;

    @Column(nullable = false)
    private boolean ageVerificationAgreed;

    @Column(nullable = false)
    private boolean privacyPolicyAgreed;

    @Column(nullable = false)
    private boolean marketingAgreed;

    @Builder.Default
    @Column(length = 10, nullable = false)
    private String serviceTermsVersion = "1.0";

    @Builder.Default
    @Column(length = 10, nullable = false)
    private String privacyPolicyVersion = "1.0";

    @Builder.Default
    @Column(length = 10, nullable = false)
    private String marketingVersion = "1.0";

    private OffsetDateTime serviceTermsAgreedAt;

    private OffsetDateTime ageVerificationAgreedAt;

    private OffsetDateTime privacyPolicyAgreedAt;

    private OffsetDateTime marketingAgreedAt;

    @Column(nullable = false)
    private boolean isSocialSignupComplete;

    @Column(length = 1)
    private String pathToWebridge;

    @Column(length = 100)
    private String pathToWebridgeOther;

    @Column(length = 1)
    private String occupation;

    @Column(length = 100)
    private String occupationOther;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public boolean hasRequiredAgreements() {
        return serviceTermsAgreed && ageVerificationAgreed && privacyPolicyAgreed;
    }

    public boolean needsSocialTermsAgreement() {
        if ("email".equals(provider)) {
            return false;
        }
        return !isSocialSignupComplete || !hasRequiredAgreements();
    }

    public boolean hasSocialProvider() {
        return !"email".equals(provider);
    }
}
