package com.webridge.feedback;

import com.webridge.common.dto.PaginatedResponse;
import com.webridge.common.exception.ApiException;
import com.webridge.feedback.dto.FeedbackCreateRequest;
import com.webridge.feedback.dto.FeedbackDto;
import com.webridge.feedback.dto.FeedbackUpdateRequest;
import com.webridge.feedback.entity.Feedback;
import com.webridge.feedback.repository.FeedbackRepository;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeedbackDto createFeedback(Long userId, FeedbackCreateRequest req) {
        Map<String, List<String>> errors = new LinkedHashMap<>();

        if (req == null || req.content() == null || req.content().isBlank()) {
            errors.put("content", List.of("이 필드는 필수 항목입니다."));
        }

        if (req == null || req.rating() == null) {
            errors.put("rating", List.of("이 필드는 필수 항목입니다."));
        } else if (req.rating() < 1 || req.rating() > 5) {
            errors.put("rating", List.of("별점은 1점부터 5점까지만 가능합니다."));
        }

        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, errors);
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));

        Feedback feedback = Feedback.builder()
            .user(user)
            .content(req.content())
            .rating(req.rating())
            .build();

        feedbackRepository.save(feedback);
        return FeedbackDto.from(feedback);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<FeedbackDto> listFeedbacks(Long userId, int page, int pageSize, String requestUri) {
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;
        if (page < 1) page = 1;

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<Feedback> feedbackPage = feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        long total = feedbackPage.getTotalElements();
        int totalPages = feedbackPage.getTotalPages();

        String next = (page < totalPages) ? (requestUri + "?page=" + (page + 1) + "&page_size=" + pageSize) : null;
        String previous = (page > 1) ? (requestUri + "?page=" + (page - 1) + "&page_size=" + pageSize) : null;

        List<FeedbackDto> results = feedbackPage.getContent().stream()
            .map(FeedbackDto::from)
            .toList();

        return new PaginatedResponse<>(total, next, previous, results);
    }

    @Transactional(readOnly = true)
    public FeedbackDto getFeedback(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findByIdAndUserId(feedbackId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "피드백을 찾을 수 없습니다.")));
        return FeedbackDto.from(feedback);
    }

    @Transactional
    public FeedbackDto updateFeedback(Long userId, Long feedbackId, FeedbackUpdateRequest req) {
        Feedback feedback = feedbackRepository.findByIdAndUserId(feedbackId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "피드백을 찾을 수 없습니다.")));

        if (req != null && req.rating() != null && (req.rating() < 1 || req.rating() > 5)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("rating", List.of("별점은 1점부터 5점까지만 가능합니다.")));
        }

        if (req != null && req.content() != null) {
            feedback.setContent(req.content());
        }
        if (req != null && req.rating() != null) {
            feedback.setRating(req.rating());
        }

        feedbackRepository.save(feedback);
        return FeedbackDto.from(feedback);
    }

    @Transactional
    public void deleteFeedback(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findByIdAndUserId(feedbackId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "피드백을 찾을 수 없습니다.")));
        feedbackRepository.delete(feedback);
    }
}
