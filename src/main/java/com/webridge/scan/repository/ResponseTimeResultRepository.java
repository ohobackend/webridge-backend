package com.webridge.scan.repository;

import com.webridge.scan.entity.ResponseTimeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResponseTimeResultRepository extends JpaRepository<ResponseTimeResult, Long> {
    List<ResponseTimeResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
