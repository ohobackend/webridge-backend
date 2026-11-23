package com.webridge.alttextfeedback.repository;

import com.webridge.alttextfeedback.entity.AltTextFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AltTextFeedbackRepository extends JpaRepository<AltTextFeedback, Long>, JpaSpecificationExecutor<AltTextFeedback> {
    List<AltTextFeedback> findByAltTextResultId(Long altTextResultId);
    List<AltTextFeedback> findByUserId(Long userId);
    Optional<AltTextFeedback> findByAltTextResultIdAndUserId(Long altTextResultId, Long userId);
    long countByRatingIgnoreCase(String rating);
}
