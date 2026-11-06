package com.webridge.scan.repository;

import com.webridge.scan.entity.ScanReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScanReportRepository extends JpaRepository<ScanReport, Long> {
    Optional<ScanReport> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
