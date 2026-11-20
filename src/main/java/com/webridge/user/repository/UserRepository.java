package com.webridge.user.repository;

import com.webridge.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByEmail(String email);

    // 관리자용 검색 & 통계
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:isAdmin IS NULL OR u.isAdmin = :isAdmin) AND " +
           "(:isActive IS NULL OR u.isActive = :isActive) AND " +
           "(:provider IS NULL OR LOWER(u.provider) = LOWER(:provider))")
    Page<User> searchUsers(
        @Param("search") String search,
        @Param("isAdmin") Boolean isAdmin,
        @Param("isActive") Boolean isActive,
        @Param("provider") String provider,
        Pageable pageable
    );

    long countByCreatedAtAfter(OffsetDateTime dateTime);
    long countByIsActiveTrue();
    long countByIsAdminTrue();
    long countByPathToWebridgeIsNotNull();

    List<User> findTop5ByOrderByCreatedAtDesc();
}
