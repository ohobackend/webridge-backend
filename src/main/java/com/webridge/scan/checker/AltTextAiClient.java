package com.webridge.scan.checker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.config.WebridgeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Component
@Slf4j
public class AltTextAiClient {

    private final WebridgeProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Semaphore rateLimiter = new Semaphore(5);

    private final String generateSystemPrompt;
    private final String evaluateSystemPrompt;

    public AltTextAiClient(WebridgeProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
        this.generateSystemPrompt = loadPrompt("prompts/alt_text_generate_system.txt");
        this.evaluateSystemPrompt = loadPrompt("prompts/alt_text_evaluate_system.txt");
    }

    private static String loadPrompt(String path) {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt: {}", path, e);
            return "";
        }
    }

    public boolean isEnabled() {
        return props.openaiApiKey() != null && !props.openaiApiKey().isBlank();
    }

    public AltTextAiResult generateAltText(String imageDataOrUrl) {
        if (!isEnabled()) {
            return null;
        }

        var body = Map.of(
                "model", "gpt-4o-2024-08-06",
                "messages", List.of(
                        Map.of("role", "system", "content", generateSystemPrompt),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "image_url", "image_url", Map.of("url", imageDataOrUrl, "detail", "low"))
                        ))
                ),
                "temperature", 0.2,
                "max_tokens", 256,
                "top_p", 1,
                "frequency_penalty", 0,
                "presence_penalty", 0,
                "response_format", Map.of("type", "json_object")
        );

        rateLimiter.acquireUninterruptibly();
        try {
            JsonNode resp = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.openaiApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (resp != null && resp.has("choices") && !resp.get("choices").isEmpty()) {
                String content = resp.get("choices").get(0).path("message").path("content").asText();
                JsonNode parsed = objectMapper.readTree(content);
                String type = parsed.path("type").asText("알 수 없음");
                String grade = parsed.path("grade").asText("매우낮음");
                String reason = parsed.path("reason").asText("대체텍스트 없음");
                String improvement = parsed.path("improvement").asText("생성 실패");
                return new AltTextAiResult(type, grade, reason, improvement, false, 2);
            }
            return AltTextAiResult.error("AI 응답 비어있음", "AI 대체텍스트 생성 실패", 3);
        } catch (Exception e) {
            log.warn("AI alt text generation failed: {}", e.getMessage());
            return AltTextAiResult.error("AI 분석 실패: " + e.getMessage(), "AI 분석 오류로 인해 대체텍스트를 생성할 수 없습니다", 3);
        } finally {
            rateLimiter.release();
        }
    }

    public AltTextAiResult evaluateAltText(String imageDataOrUrl, String altText) {
        if (!isEnabled()) {
            return null;
        }

        var body = Map.of(
                "model", "gpt-4o-2024-08-06",
                "messages", List.of(
                        Map.of("role", "system", "content", evaluateSystemPrompt),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", "alt='" + altText + "'"),
                                Map.of("type", "image_url", "image_url", Map.of("url", imageDataOrUrl, "detail", "low"))
                        ))
                ),
                "temperature", 0.2,
                "max_tokens", 256,
                "top_p", 1,
                "frequency_penalty", 0,
                "presence_penalty", 0,
                "response_format", Map.of("type", "json_object")
        );

        rateLimiter.acquireUninterruptibly();
        try {
            JsonNode resp = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.openaiApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (resp != null && resp.has("choices") && !resp.get("choices").isEmpty()) {
                String content = resp.get("choices").get(0).path("message").path("content").asText();
                JsonNode parsed = objectMapper.readTree(content);
                String type = parsed.path("type").asText("알 수 없음");
                String grade = parsed.path("grade").asText("매우낮음");
                String reason = parsed.path("reason").asText("평가 없음");
                String improvement = parsed.path("improvement").asText("개선 제안 없음");
                int compliant = mapGradeToCompliance(grade);
                return new AltTextAiResult(type, grade, reason, improvement, false, compliant);
            }
            return AltTextAiResult.error("AI 평가 응답 비어있음", "AI 평가 실패", 3);
        } catch (Exception e) {
            log.warn("AI alt text evaluation failed: {}", e.getMessage());
            return AltTextAiResult.error("AI 평가 실패: " + e.getMessage(), "AI 평가 오류로 인해 대체텍스트를 평가할 수 없습니다", 3);
        } finally {
            rateLimiter.release();
        }
    }

    private static int mapGradeToCompliance(String grade) {
        return switch (grade) {
            case "준수(매우높음)" -> 0;
            case "준수(조금높음)", "준수" -> 1;
            case "미준수" -> 2;
            default -> 3;
        };
    }
}
