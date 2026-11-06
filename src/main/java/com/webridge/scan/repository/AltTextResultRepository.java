package com.webridge.scan.repository;

import com.webridge.scan.entity.AltTextResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AltTextResultRepository extends JpaRepository<AltTextResult, Long> {
    List<AltTextResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
