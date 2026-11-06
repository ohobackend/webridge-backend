package com.webridge.scan.repository;

import com.webridge.scan.entity.BasicLanguageResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BasicLanguageResultRepository extends JpaRepository<BasicLanguageResult, Long> {
    List<BasicLanguageResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
