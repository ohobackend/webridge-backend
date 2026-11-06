package com.webridge.scan.repository;

import com.webridge.scan.entity.TableResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TableResultRepository extends JpaRepository<TableResult, Long> {
    List<TableResult> findByScanId(UUID scanId);
    void deleteByScanId(UUID scanId);
}
