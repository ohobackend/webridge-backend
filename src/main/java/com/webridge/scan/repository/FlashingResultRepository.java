package com.webridge.scan.repository;

import com.webridge.scan.entity.FlashingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlashingResultRepository extends JpaRepository<FlashingResult, Long> {
    List<FlashingResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
