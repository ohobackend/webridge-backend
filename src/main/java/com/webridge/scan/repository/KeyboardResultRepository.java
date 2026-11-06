package com.webridge.scan.repository;

import com.webridge.scan.entity.KeyboardResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KeyboardResultRepository extends JpaRepository<KeyboardResult, Long> {
    List<KeyboardResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
