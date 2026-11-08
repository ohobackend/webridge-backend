package com.webridge.alttextfeedback;

import com.webridge.alttextfeedback.dto.AltTextFeedbackCreateRequest;
import com.webridge.alttextfeedback.dto.AltTextFeedbackDto;
import com.webridge.alttextfeedback.dto.AltTextFeedbackListDto;
import com.webridge.alttextfeedback.dto.AltTextResultWithFeedbackDto;
import com.webridge.alttextfeedback.entity.AltTextFeedback;
import com.webridge.alttextfeedback.repository.AltTextFeedbackRepository;
import com.webridge.common.exception.ApiException;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.AltTextResult;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.scan.repository.AltTextResultRepository;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AltTextFeedbackService {

    private final AltTextFeedbackRepository altTextFeedbackRepository;
    private final AltTextResultRepository altTextResultRepository;
    private final AccessibilityScanRepository scanRepository;
    private final UserRepository userRepository;

    public record FeedbackCreationResult(AltTextFeedbackDto dto, boolean created) {}

    @Transactional
    public FeedbackCreationResult createOrUpdateFeedback(Long userId, AltTextFeedbackCreateRequest req) {
        if (req == null || req.altTextResult() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("alt_text_result", List.of("대체텍스트 결과 ID는 필수입니다.")));
        }

        AltTextResult result = altTextResultRepository.findById(req.altTextResult())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("error", "대체텍스트 결과를 찾을 수 없습니다.")));

        if (result.getScan() == null || result.getScan().getUser() == null || !result.getScan().getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("alt_text_result", List.of("자신의 스캔 결과에만 평가할 수 있습니다.")));
        }

        if (result.getSuggestedAlt() == null || result.getSuggestedAlt().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Map.of("alt_text_result", List.of("AI 제안이 없는 결과에는 평가할 수 없습니다.")));
        }

        User user = getUserOrThrow(userId);
        Optional<AltTextFeedback> existingOpt = altTextFeedbackRepository.findByAltTextResultIdAndUserId(result.getId(), userId);

        if (existingOpt.isPresent()) {
            AltTextFeedback feedback = existingOpt.get();
            feedback.setRating(req.rating() != null ? req.rating() : "like");
            altTextFeedbackRepository.save(feedback);
            return new FeedbackCreationResult(AltTextFeedbackDto.from(feedback), false);
        } else {
            AltTextFeedback feedback = AltTextFeedback.builder()
                .altTextResult(result)
                .user(user)
                .rating(req.rating() != null ? req.rating() : "like")
                .build();
            altTextFeedbackRepository.save(feedback);
            return new FeedbackCreationResult(AltTextFeedbackDto.from(feedback), true);
        }
    }

    @Transactional(readOnly = true)
    public List<AltTextFeedbackListDto> getUserFeedbacks(Long userId) {
        List<AltTextFeedback> feedbacks = altTextFeedbackRepository.findByUserId(userId);
        return feedbacks.stream()
            .map(AltTextFeedbackListDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public AltTextFeedbackDto getFeedbackByResultId(Long userId, Long altTextResultId) {
        AltTextResult result = altTextResultRepository.findById(altTextResultId)
            .filter(r -> r.getScan() != null && r.getScan().getUser() != null && r.getScan().getUser().getId().equals(userId))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("error", "대체텍스트 결과를 찾을 수 없습니다.")));

        AltTextFeedback feedback = altTextFeedbackRepository.findByAltTextResultIdAndUserId(altTextResultId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("error", "평가를 찾을 수 없습니다.")));

        return AltTextFeedbackDto.from(feedback);
    }

    @Transactional
    public void deleteFeedback(Long userId, Long feedbackId) {
        AltTextFeedback feedback = altTextFeedbackRepository.findById(feedbackId)
            .filter(f -> f.getUser() != null && f.getUser().getId().equals(userId))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("error", "평가를 찾을 수 없습니다.")));

        altTextFeedbackRepository.delete(feedback);
    }

    @Transactional(readOnly = true)
    public List<AltTextResultWithFeedbackDto> getScanAltTextResultsWithFeedback(Long userId, UUID scanId) {
        scanRepository.findByIdAndUserId(scanId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("error", "스캔을 찾을 수 없습니다.")));

        List<AltTextResult> results = altTextResultRepository.findByScanId(scanId);
        Map<Long, String> userFeedbacks = altTextFeedbackRepository.findByUserId(userId).stream()
            .collect(Collectors.toMap(f -> f.getAltTextResult().getId(), AltTextFeedback::getRating, (a, b) -> b));

        return results.stream().map(r -> new AltTextResultWithFeedbackDto(
            r.getId(),
            r.getImgUrl(),
            r.getElementHtml(),
            r.getAltText(),
            r.getSuggestedAlt(),
            r.getCompliance(),
            AltTextResultWithFeedbackDto.getComplianceDisplay(r.getCompliance()),
            r.getCreatedAt(),
            userFeedbacks.get(r.getId()),
            r.getSuggestedAlt() != null && !r.getSuggestedAlt().isBlank(),
            r.getAiType(),
            r.getAiGrade(),
            r.getAiReason(),
            r.getAiImprovement(),
            r.isHasError()
        )).toList();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));
    }
}
