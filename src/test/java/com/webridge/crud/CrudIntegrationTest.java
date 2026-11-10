package com.webridge.crud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.alttextfeedback.dto.AltTextFeedbackCreateRequest;
import com.webridge.alttextfeedback.repository.AltTextFeedbackRepository;
import com.webridge.auth.jwt.JwtProvider;
import com.webridge.feedback.dto.FeedbackCreateRequest;
import com.webridge.feedback.dto.FeedbackUpdateRequest;
import com.webridge.feedback.repository.FeedbackRepository;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.entity.AltTextResult;
import com.webridge.scan.entity.ScanStatus;
import com.webridge.scan.repository.AccessibilityScanRepository;
import com.webridge.scan.repository.AltTextResultRepository;
import com.webridge.survey.dto.SurveyCreateRequest;
import com.webridge.survey.repository.SurveyRepository;
import com.webridge.survey.repository.SurveyTriggerRepository;
import com.webridge.terms.dto.TermsUpdateRequest;
import com.webridge.terms.repository.TermsRepository;
import com.webridge.user.dto.UserExtraInfoRequest;
import com.webridge.user.dto.UserUpdateRequest;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("schematest")
class CrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyTriggerRepository surveyTriggerRepository;

    @Autowired
    private AccessibilityScanRepository scanRepository;

    @Autowired
    private AltTextResultRepository altTextResultRepository;

    @Autowired
    private AltTextFeedbackRepository altTextFeedbackRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() {
        altTextFeedbackRepository.deleteAll();
        altTextResultRepository.deleteAll();
        scanRepository.deleteAll();
        surveyRepository.deleteAll();
        surveyTriggerRepository.deleteAll();
        feedbackRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
            .email("crudtest@example.com")
            .name("테스터")
            .password("pbkdf2_sha256$1000000$saltsalt$fakehash")
            .provider("email")
            .isActive(true)
            .serviceTermsAgreed(true)
            .privacyPolicyAgreed(true)
            .marketingAgreed(false)
            .serviceTermsVersion("1.0")
            .privacyPolicyVersion("1.0")
            .marketingVersion("1.0")
            .build());

        token = jwtProvider.createAccess(testUser.getId());
    }

    // ==========================================
    // 1. User Domain Tests
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/user/me - 현재 사용자 프로필 조회 성공")
    void getUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("사용자 정보를 성공적으로 조회했습니다."))
            .andExpect(jsonPath("$.data.email").value("crudtest@example.com"))
            .andExpect(jsonPath("$.data.name").value("테스터"))
            .andExpect(jsonPath("$.data.provider").value("email"));
    }

    @Test
    @DisplayName("PATCH /api/v1/user/me - 프로필 이름 수정 성공 및 유효성 검사")
    void updateUserProfile() throws Exception {
        // 이름 2자 미만 시 400
        mockMvc.perform(patch("/api/v1/user/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserUpdateRequest("A"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors.name[0]").value("이름은 최소 2자 이상이어야 합니다."));

        // 정상 수정
        mockMvc.perform(patch("/api/v1/user/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserUpdateRequest("김새이름"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("김새이름"));
    }

    @Test
    @DisplayName("POST /api/v1/user/extra-info/ - 추가 정보 저장 및 기타 검증")
    void updateExtraInfo() throws Exception {
        // '기타' 선택 후 상세 미입력 시 400
        mockMvc.perform(post("/api/v1/user/extra-info/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserExtraInfoRequest("f", "", "a", null))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.path_to_webridge_other[0]").value("기타 선택 시 필수 입력입니다."));

        // 정상 저장
        mockMvc.perform(post("/api/v1/user/extra-info/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UserExtraInfoRequest("b", null, "c", null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.path_to_webridge").value("b"))
            .andExpect(jsonPath("$.occupation").value("c"));
    }

    @Test
    @DisplayName("DELETE /api/v1/user/me - 회원 탈퇴 시 계정 및 연관된 모든 스캔, 피드백, 설문 데이터 삭제")
    void deleteUserAccount() throws Exception {
        AccessibilityScan completedScan = scanRepository.save(AccessibilityScan.builder()
            .user(testUser)
            .url("https://example.com")
            .status(ScanStatus.COMPLETED)
            .build());

        AltTextResult altResult = altTextResultRepository.save(AltTextResult.builder()
            .scan(completedScan)
            .imgUrl("https://example.com/test.png")
            .compliance(1)
            .build());

        altTextFeedbackRepository.save(com.webridge.alttextfeedback.entity.AltTextFeedback.builder()
            .altTextResult(altResult)
            .user(testUser)
            .rating("good")
            .build());

        feedbackRepository.save(com.webridge.feedback.entity.Feedback.builder()
            .user(testUser)
            .content("좋은 서비스입니다.")
            .rating(5)
            .build());

        surveyTriggerRepository.save(com.webridge.survey.entity.SurveyTrigger.builder()
            .user(testUser)
            .completedScansCount(1)
            .build());

        mockMvc.perform(delete("/api/v1/user/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.deleted_email").value("crudtest@example.com"));

        assertThat(userRepository.findById(testUser.getId())).isEmpty();
        assertThat(scanRepository.findById(completedScan.getId())).isEmpty();
        assertThat(altTextResultRepository.findById(altResult.getId())).isEmpty();
        assertThat(altTextFeedbackRepository.findAll()).isEmpty();
        assertThat(feedbackRepository.findAll()).isEmpty();
        assertThat(surveyTriggerRepository.findAll()).isEmpty();
    }

    // ==========================================
    // 2. Terms Domain Tests
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/terms/current/ - 현재 약관 조회 (비인증 허용)")
    void getCurrentTerms() throws Exception {
        mockMvc.perform(get("/api/v1/terms/current/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").exists())
            .andExpect(jsonPath("$.privacy").exists())
            .andExpect(jsonPath("$.marketing").exists());
    }

    @Test
    @DisplayName("GET /api/v1/terms/status/ & POST /api/v1/terms/update/ - 약관 상태 및 재동의")
    void checkAndUpdateTermsStatus() throws Exception {
        // 상태 확인
        mockMvc.perform(get("/api/v1/terms/status/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.needs_update").value(false))
            .andExpect(jsonPath("$.user_agreements.service_terms").value(true));

        // 재동의
        mockMvc.perform(post("/api/v1/terms/update/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TermsUpdateRequest(true, true, true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("약관 동의가 업데이트되었습니다."))
            .andExpect(jsonPath("$.user_agreements.marketing").value(true));
    }

    // ==========================================
    // 3. Feedback Domain Tests
    // ==========================================

    @Test
    @DisplayName("Feedback CRUD - 피드백 생성, 목록, 단건 조회, 수정, 삭제 흐름")
    void feedbackCrudFlow() throws Exception {
        // 1. 별점 범위 오류 검증 (6점)
        mockMvc.perform(post("/api/v1/feedback/create/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FeedbackCreateRequest("너무 좋아요", 6))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.rating[0]").value("별점은 1점부터 5점까지만 가능합니다."));

        // 2. 피드백 정상 생성
        MvcResult createResult = mockMvc.perform(post("/api/v1/feedback/create/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FeedbackCreateRequest("아주 유용한 서비스입니다.", 5))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.content").value("아주 유용한 서비스입니다."))
            .andExpect(jsonPath("$.rating").value(5))
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long feedbackId = objectMapper.readTree(responseBody).get("id").asLong();

        // 3. 피드백 목록 조회 (페이지네이션)
        mockMvc.perform(get("/api/v1/feedback/list/")
                .header("Authorization", "Bearer " + token)
                .param("page", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.results[0].id").value(feedbackId));

        // 4. 피드백 상세 조회
        mockMvc.perform(get("/api/v1/feedback/" + feedbackId + "/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(feedbackId))
            .andExpect(jsonPath("$.rating").value(5));

        // 5. 피드백 수정
        mockMvc.perform(patch("/api/v1/feedback/" + feedbackId + "/update/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new FeedbackUpdateRequest("수정된 피드백 내용", 4))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("수정된 피드백 내용"))
            .andExpect(jsonPath("$.rating").value(4));

        // 6. 피드백 삭제
        mockMvc.perform(delete("/api/v1/feedback/" + feedbackId + "/delete/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        // 7. 삭제 후 조회 시 404
        mockMvc.perform(get("/api/v1/feedback/" + feedbackId + "/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("피드백을 찾을 수 없습니다."));
    }

    // ==========================================
    // 4. Survey Domain Tests
    // ==========================================

    @Test
    @DisplayName("Survey - 설문 트리거 확인, 표시 완료 기록, 설문 제출")
    void surveyFlow() throws Exception {
        // 1. 초기 트리거 상태 확인
        mockMvc.perform(get("/api/v1/survey/check-trigger/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.survey_completed").value(false))
            .andExpect(jsonPath("$.should_show_survey").value(false));

        // 2. 설문 팝업 표시됨 기록
        mockMvc.perform(post("/api/v1/survey/mark-shown/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("설문조사 팝업 표시됨이 기록되었습니다."));

        // 3. 설문 제출 (유효성 검사 실패: 개인정보 미동의)
        SurveyCreateRequest invalidReq = new SurveyCreateRequest(
            "crudtest@example.com", false,
            List.of("a"), "", List.of("a"), "",
            5, 5, 5, 5, "a", "", "a", "",
            List.of("a"), "", List.of("a"), "", "개선의견"
        );
        mockMvc.perform(post("/api/v1/survey/create/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReq)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.privacy_agreed[0]").value("개인정보 수집 이용에 동의해야 합니다."));

        // 4. 설문 정상 제출
        SurveyCreateRequest validReq = new SurveyCreateRequest(
            "crudtest@example.com", true,
            List.of("a"), "", List.of("b"), "",
            5, 4, 5, 5, "a", "", "a", "",
            List.of("a"), "", List.of("a"), "", "훌륭합니다."
        );
        mockMvc.perform(post("/api/v1/survey/create/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.privacy_agreed").value(true));

        // 5. 제출 후 트리거 상태 확인 (survey_completed=true)
        mockMvc.perform(get("/api/v1/survey/check-trigger/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.survey_completed").value(true));
    }

    // ==========================================
    // 5. AltTextFeedback Domain Tests
    // ==========================================

    @Test
    @DisplayName("AltTextFeedback - 대체텍스트 평가 생성, 갱신, 목록 및 스캔 결과 조회")
    void altTextFeedbackFlow() throws Exception {
        AccessibilityScan scan = scanRepository.save(AccessibilityScan.builder()
            .user(testUser)
            .url("https://example.com/test")
            .title("테스트 스캔")
            .status(ScanStatus.COMPLETED)
            .build());

        AltTextResult result = altTextResultRepository.save(AltTextResult.builder()
            .scan(scan)
            .imgUrl("https://example.com/img1.png")
            .elementHtml("<img src='https://example.com/img1.png'>")
            .altText("오래된 텍스트")
            .suggestedAlt("AI가 추천한 새로운 대체텍스트")
            .compliance(1)
            .build());

        // 1. 평가 생성 (like) -> 201 Created
        mockMvc.perform(post("/api/v1/alt-text-feedback/create/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AltTextFeedbackCreateRequest(result.getId(), "like"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.rating").value("like"))
            .andExpect(jsonPath("$.alt_text_result").value(result.getId()));

        // 2. 동일 결과 재평가 (dislike) -> 200 OK (업데이트)
        mockMvc.perform(post("/api/v1/alt-text-feedback/create/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AltTextFeedbackCreateRequest(result.getId(), "dislike"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rating").value("dislike"));

        // 3. 내 평가 목록 조회
        mockMvc.perform(get("/api/v1/alt-text-feedback/my/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].rating").value("dislike"))
            .andExpect(jsonPath("$[0].suggested_alt").value("AI가 추천한 새로운 대체텍스트"));

        // 4. 특정 결과의 평가 조회
        mockMvc.perform(get("/api/v1/alt-text-feedback/result/" + result.getId() + "/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rating").value("dislike"));

        // 5. 스캔별 대체텍스트 결과와 사용자 평가 함께 조회
        mockMvc.perform(get("/api/v1/scans/" + scan.getId() + "/alt-text-results/")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(result.getId()))
            .andExpect(jsonPath("$[0].user_feedback").value("dislike"))
            .andExpect(jsonPath("$[0].compliance_display").value("개선 필요"))
            .andExpect(jsonPath("$[0].has_ai_suggestion").value(true));
    }
}
