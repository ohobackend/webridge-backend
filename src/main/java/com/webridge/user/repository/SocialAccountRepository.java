package com.webridge.user.repository;

import com.webridge.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);
    List<SocialAccount> findByUserId(Long userId);
    boolean existsByProviderAndProviderId(String provider, String providerId);
}
