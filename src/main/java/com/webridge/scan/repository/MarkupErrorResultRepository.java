package com.webridge.scan.repository;

import com.webridge.scan.entity.MarkupErrorResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarkupErrorResultRepository extends JpaRepository<MarkupErrorResult, Long> {
    List<MarkupErrorResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
