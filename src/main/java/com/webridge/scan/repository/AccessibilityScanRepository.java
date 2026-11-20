package com.webridge.scan.repository;

import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessibilityScanRepository extends JpaRepository<AccessibilityScan, UUID> {
    Page<AccessibilityScan> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<AccessibilityScan> findByUserId(Long userId, Pageable pageable);
    Page<AccessibilityScan> findByUserIdAndStatus(Long userId, ScanStatus status, Pageable pageable);
    Optional<AccessibilityScan> findByIdAndUserId(UUID id, Long userId);
    List<AccessibilityScan> findByUserIdAndStatus(Long userId, ScanStatus status);
    List<AccessibilityScan> findByStatusInAndUpdatedAtBefore(List<ScanStatus> statuses, OffsetDateTime threshold);
    long countByUserId(Long userId);

    @Query("SELECT s FROM AccessibilityScan s JOIN FETCH s.user WHERE s.id = :id")
    Optional<AccessibilityScan> findWithUserById(@Param("id") UUID id);

    // 관리자용 메서드
    @Query("SELECT s FROM AccessibilityScan s LEFT JOIN s.user u WHERE " +
           "(:search IS NULL OR LOWER(s.url) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) OR (u IS NOT NULL AND LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))) AND " +
           "(:status IS NULL OR s.status = :status)")
    Page<AccessibilityScan> searchScans(
        @Param("search") String search,
        @Param("status") ScanStatus status,
        Pageable pageable
    );

    long countByCreatedAtAfter(OffsetDateTime dateTime);

    long countByStatus(ScanStatus status);

    @Query("SELECT AVG(s.complianceScore) FROM AccessibilityScan s WHERE s.status = com.webridge.scan.entity.ScanStatus.COMPLETED")
    Double findAverageComplianceScore();

    List<AccessibilityScan> findTop5ByOrderByCreatedAtDesc();

    List<AccessibilityScan> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
