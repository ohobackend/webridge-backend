package com.webridge.survey.repository;

import com.webridge.survey.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {
    List<Survey> findByUserIdOrderByCreatedAtDesc(Long userId);
}
