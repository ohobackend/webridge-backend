package com.webridge.scan.repository;

import com.webridge.scan.entity.VideoCaptionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoCaptionResultRepository extends JpaRepository<VideoCaptionResult, Long> {
    List<VideoCaptionResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
