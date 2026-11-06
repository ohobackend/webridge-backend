package com.webridge.scan.repository;

import com.webridge.scan.entity.HeadingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HeadingResultRepository extends JpaRepository<HeadingResult, Long> {
    List<HeadingResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
