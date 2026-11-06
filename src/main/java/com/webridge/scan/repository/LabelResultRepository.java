package com.webridge.scan.repository;

import com.webridge.scan.entity.LabelResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabelResultRepository extends JpaRepository<LabelResult, Long> {
    List<LabelResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
