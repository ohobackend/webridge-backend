package com.webridge.user.repository;

import com.webridge.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByEmail(String email);

    long countByCreatedAtAfter(OffsetDateTime dateTime);
    long countByIsActiveTrue();
    long countByIsAdminTrue();
    long countByPathToWebridgeIsNotNull();

    List<User> findTop5ByOrderByCreatedAtDesc();
}
