package com.webridge.scan.scheduler;

import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.progress.ScanProgressPublisher;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.scan.repository.AltTextResultRepository;
import com.webridge.scan.repository.BasicLanguageResultRepository;
import com.webridge.scan.repository.ContrastResultRepository;
import com.webridge.scan.repository.FlashingResultRepository;
import com.webridge.scan.repository.HeadingResultRepository;
import com.webridge.scan.repository.KeyboardResultRepository;
import com.webridge.scan.repository.LabelResultRepository;
import com.webridge.scan.repository.MarkupErrorResultRepository;
import com.webridge.scan.repository.PauseControlResultRepository;
import com.webridge.scan.repository.ResponseTimeResultRepository;
import com.webridge.scan.repository.TableResultRepository;
import com.webridge.scan.repository.VideoAutoplayResultRepository;
import com.webridge.scan.repository.VideoCaptionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanCleanupService {

    private final AccessibilityScanRepository scanRepo;
    private final ScanProgressPublisher progressPublisher;

    private final AltTextResultRepository altTextRepo;
    private final ContrastResultRepository contrastRepo;
    private final KeyboardResultRepository keyboardRepo;
    private final LabelResultRepository labelRepo;
    private final TableResultRepository tableRepo;
    private final VideoCaptionResultRepository videoCaptionRepo;
    private final VideoAutoplayResultRepository videoAutoplayRepo;
    private final BasicLanguageResultRepository basicLanguageRepo;
    private final MarkupErrorResultRepository markupErrorRepo;
    private final HeadingResultRepository headingRepo;
    private final ResponseTimeResultRepository responseTimeRepo;
    private final PauseControlResultRepository pauseControlRepo;
    private final FlashingResultRepository flashingRepo;

    @Transactional
    public int cleanup(int timeoutMinutes) {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(timeoutMinutes);
        List<AccessibilityScan> stuckScans = scanRepo.findAll().stream()
                .filter(s -> s.getStatus() == ScanStatus.PROCESSING && s.getUpdatedAt() != null && s.getUpdatedAt().isBefore(threshold))
                .toList();

        if (stuckScans.isEmpty()) {
            return 0;
        }

        log.info("Found {} stuck scans (timeout: {}m)", stuckScans.size(), timeoutMinutes);

        int cleaned = 0;
        for (AccessibilityScan scan : stuckScans) {
            try {
                // Delete partial results
                altTextRepo.deleteByScanId(scan.getId());
                contrastRepo.deleteByScanId(scan.getId());
                keyboardRepo.deleteByScanId(scan.getId());
                labelRepo.deleteByScanId(scan.getId());
                tableRepo.deleteByScanId(scan.getId());
                videoCaptionRepo.deleteByScanId(scan.getId());
                videoAutoplayRepo.deleteByScanId(scan.getId());
                basicLanguageRepo.deleteByScanId(scan.getId());
                markupErrorRepo.deleteByScanId(scan.getId());
                headingRepo.deleteByScanId(scan.getId());
                responseTimeRepo.deleteByScanId(scan.getId());
                pauseControlRepo.deleteByScanId(scan.getId());
                flashingRepo.deleteByScanId(scan.getId());

                // Reset scan state to failed
                scan.setStatus(ScanStatus.FAILED);
                scan.setErrorMessage("검사가 중단되어 자동으로 정리되었습니다 (타임아웃)");
                scan.setProgress(0);
                scan.setTotalIssues(0);
                scan.setComplianceScore(0.0);
                scan.setAltTextCompleted(false);
                scan.setContrastCompleted(false);
                scan.setKeyboardCompleted(false);
                scan.setLabelCompleted(false);
                scan.setTableCompleted(false);
                scan.setVideoCaptionCompleted(false);
                scan.setVideoAutoplayCompleted(false);
                scan.setBasicLanguageCompleted(false);
                scan.setMarkupErrorCompleted(false);
                scan.setHeadingCompleted(false);
                scan.setResponseTimeCompleted(false);
                scan.setPauseControlCompleted(false);
                scan.setFlashingCompleted(false);
                scanRepo.save(scan);

                progressPublisher.error(scan.getId(), "검사가 중단되어 자동으로 정리되었습니다 (타임아웃)");
                cleaned++;
            } catch (Exception e) {
                log.error("Failed to clean up stuck scan {}: {}", scan.getId(), e.getMessage());
            }
        }

        return cleaned;
    }
}
