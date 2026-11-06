package com.webridge.alttextfeedback.repository;

import com.webridge.alttextfeedback.entity.AltTextFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AltTextFeedbackRepository extends JpaRepository<AltTextFeedback, Long> {
    List<AltTextFeedback> findByAltTextResultId(Long altTextResultId);
    List<AltTextFeedback> findByUserId(Long userId);
    Optional<AltTextFeedback> findByAltTextResultIdAndUserId(Long altTextResultId, Long userId);
}
