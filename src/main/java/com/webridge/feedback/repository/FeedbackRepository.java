package com.webridge.feedback.repository;

import com.webridge.feedback.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Feedback> findByIdAndUserId(Long id, Long userId);
}

