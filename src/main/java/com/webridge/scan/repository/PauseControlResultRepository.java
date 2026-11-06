package com.webridge.scan.repository;

import com.webridge.scan.entity.PauseControlResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PauseControlResultRepository extends JpaRepository<PauseControlResult, Long> {
    List<PauseControlResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
