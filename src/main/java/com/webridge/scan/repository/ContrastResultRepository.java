package com.webridge.scan.repository;

import com.webridge.scan.entity.ContrastResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContrastResultRepository extends JpaRepository<ContrastResult, Long> {
    List<ContrastResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
