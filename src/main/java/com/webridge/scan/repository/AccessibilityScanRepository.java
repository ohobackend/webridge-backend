package com.webridge.scan.repository;

import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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

    @org.springframework.data.jpa.repository.Query("SELECT s FROM AccessibilityScan s JOIN FETCH s.user WHERE s.id = :id")
    Optional<AccessibilityScan> findWithUserById(@org.springframework.data.repository.query.Param("id") UUID id);
}
