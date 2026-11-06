package com.webridge.survey.repository;

import com.webridge.survey.entity.SurveyTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyTriggerRepository extends JpaRepository<SurveyTrigger, Long> {
    Optional<SurveyTrigger> findByUserId(Long userId);
}
