package com.webridge.scan.repository;

import com.webridge.scan.entity.VideoAutoplayResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoAutoplayResultRepository extends JpaRepository<VideoAutoplayResult, Long> {
    List<VideoAutoplayResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
