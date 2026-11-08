package com.webridge.terms;

import com.webridge.common.exception.ApiException;
import com.webridge.terms.dto.TermsDto;
import com.webridge.terms.dto.TermsUpdateRequest;
import com.webridge.terms.dto.TermsUpdateResponse;
import com.webridge.terms.entity.Terms;
import com.webridge.terms.repository.TermsRepository;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsRepository termsRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentTerms() {
        Map<String, Object> termsData = new LinkedHashMap<>();
        List<String> types = List.of("service", "privacy", "marketing");
        Map<String, String> names = Map.of(
            "service", "서비스 이용약관",
            "privacy", "개인정보 처리방침",
            "marketing", "마케팅 활용 동의"
        );

        for (String type : types) {
            Optional<Terms> termsOpt = termsRepository.findByTypeAndIsActiveTrue(type);
            if (termsOpt.isPresent()) {
                termsData.put(type, TermsDto.from(termsOpt.get()));
            } else {
                Map<String, Object> fallback = new LinkedHashMap<>();
                fallback.put("type", type);
                fallback.put("version", "1.0");
                fallback.put("content", names.getOrDefault(type, type) + " 내용이 준비중입니다.");
                fallback.put("created_at", OffsetDateTime.now().toString());
                termsData.put(type, fallback);
            }
        }

        return termsData;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkTermsStatus(Long userId) {
        User user = getUserOrThrow(userId);

        Optional<Terms> serviceTerms = termsRepository.findByTypeAndIsActiveTrue("service");
        Optional<Terms> privacyTerms = termsRepository.findByTypeAndIsActiveTrue("privacy");
        Optional<Terms> marketingTerms = termsRepository.findByTypeAndIsActiveTrue("marketing");

        List<String> outdatedTerms = new ArrayList<>();
        List<String> outdatedNames = new ArrayList<>();
        Map<String, String> nameMap = Map.of(
            "service", "서비스 이용약관",
            "privacy", "개인정보 처리방침",
            "marketing", "마케팅 활용 동의"
        );

        if (user.isServiceTermsAgreed() && serviceTerms.isPresent()) {
            Terms st = serviceTerms.get();
            if (!st.getVersion().equals(user.getServiceTermsVersion()) && st.isRequiresReagreement()) {
                outdatedTerms.add("service");
                outdatedNames.add(nameMap.get("service"));
            }
        }

        if (user.isPrivacyPolicyAgreed() && privacyTerms.isPresent()) {
            Terms pt = privacyTerms.get();
            if (!pt.getVersion().equals(user.getPrivacyPolicyVersion()) && pt.isRequiresReagreement()) {
                outdatedTerms.add("privacy");
                outdatedNames.add(nameMap.get("privacy"));
            }
        }

        if (user.isMarketingAgreed() && marketingTerms.isPresent()) {
            Terms mt = marketingTerms.get();
            if (!mt.getVersion().equals(user.getMarketingVersion()) && mt.isRequiresReagreement()) {
                outdatedTerms.add("marketing");
                outdatedNames.add(nameMap.get("marketing"));
            }
        }

        if (!outdatedTerms.isEmpty()) {
            Map<String, Object> updateRes = new LinkedHashMap<>();
            updateRes.put("needs_update", true);
            updateRes.put("outdated_terms", outdatedTerms);
            updateRes.put("message", String.join(", ", outdatedNames) + "이(가) 업데이트되었습니다. 재동의가 필요합니다.");
            return updateRes;
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("needs_update", false);
        res.put("user_agreements", Map.of(
            "service_terms", user.isServiceTermsAgreed(),
            "privacy_policy", user.isPrivacyPolicyAgreed(),
            "marketing", user.isMarketingAgreed()
        ));
        res.put("user_versions", Map.of(
            "service", user.getServiceTermsVersion() != null ? user.getServiceTermsVersion() : "1.0",
            "privacy", user.getPrivacyPolicyVersion() != null ? user.getPrivacyPolicyVersion() : "1.0",
            "marketing", user.getMarketingVersion() != null ? user.getMarketingVersion() : "1.0"
        ));
        res.put("current_versions", Map.of(
            "service", serviceTerms.map(Terms::getVersion).orElse("1.0"),
            "privacy", privacyTerms.map(Terms::getVersion).orElse("1.0"),
            "marketing", marketingTerms.map(Terms::getVersion).orElse("1.0")
        ));
        return res;
    }

    @Transactional
    public TermsUpdateResponse updateTermsAgreement(Long userId, TermsUpdateRequest req) {
        User user = getUserOrThrow(userId);
        OffsetDateTime now = OffsetDateTime.now();

        if (Boolean.TRUE.equals(req.serviceTermsAgreed())) {
            user.setServiceTermsAgreed(true);
            user.setServiceTermsAgreedAt(now);
            termsRepository.findByTypeAndIsActiveTrue("service").ifPresent(t -> user.setServiceTermsVersion(t.getVersion()));
        }

        if (Boolean.TRUE.equals(req.privacyPolicyAgreed())) {
            user.setPrivacyPolicyAgreed(true);
            user.setPrivacyPolicyAgreedAt(now);
            termsRepository.findByTypeAndIsActiveTrue("privacy").ifPresent(t -> user.setPrivacyPolicyVersion(t.getVersion()));
        }

        if (req.marketingAgreed() != null) {
            user.setMarketingAgreed(req.marketingAgreed());
            user.setMarketingAgreedAt(req.marketingAgreed() ? now : null);
            if (req.marketingAgreed()) {
                termsRepository.findByTypeAndIsActiveTrue("marketing").ifPresent(t -> user.setMarketingVersion(t.getVersion()));
            }
        }

        userRepository.save(user);

        return new TermsUpdateResponse(
            "약관 동의가 업데이트되었습니다.",
            Map.of(
                "service_terms", user.isServiceTermsAgreed(),
                "privacy_policy", user.isPrivacyPolicyAgreed(),
                "marketing", user.isMarketingAgreed()
            )
        );
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Map.of("detail", "사용자를 찾을 수 없습니다.")));
    }
}
