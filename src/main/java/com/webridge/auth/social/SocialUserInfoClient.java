package com.webridge.auth.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.webridge.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class SocialUserInfoClient {

    private final RestClient rest;

    public SocialUserInfoClient() {
        this.rest = RestClient.create();
    }

    public SocialUserInfoClient(RestClient rest) {
        this.rest = rest;
    }

    public SocialUserInfo fetch(String provider, String accessToken) {
        if (provider == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 제공자입니다.");
        }
        return switch (provider.toLowerCase()) {
            case "google" -> google(accessToken);
            case "kakao" -> kakao(accessToken);
            case "github" -> github(accessToken);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 제공자: " + provider);
        };
    }

    private SocialUserInfo google(String token) {
        try {
            JsonNode d = rest.get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(JsonNode.class);

            if (d == null || !d.has("id")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Google 사용자 정보를 가져올 수 없습니다.");
            }

            return new SocialUserInfo(
                d.path("id").asText(),
                d.path("email").asText(""),
                d.path("name").asText(""),
                d.path("picture").asText("")
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google user info fetch error", e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google 사용자 정보를 가져올 수 없습니다.");
        }
    }

    private SocialUserInfo kakao(String token) {
        try {
            JsonNode d = rest.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(JsonNode.class);

            if (d == null || !d.has("id")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Kakao 사용자 정보를 가져올 수 없습니다.");
            }

            JsonNode acc = d.path("kakao_account");
            JsonNode profile = acc.path("profile");

            return new SocialUserInfo(
                d.path("id").asText(),
                acc.path("email").asText(""),
                profile.path("nickname").asText(""),
                profile.path("profile_image_url").asText("")
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Kakao user info fetch error", e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Kakao 사용자 정보를 가져올 수 없습니다.");
        }
    }

    private SocialUserInfo github(String token) {
        try {
            JsonNode d = rest.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .body(JsonNode.class);

            if (d == null || !d.has("id")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "GitHub 사용자 정보를 가져올 수 없습니다.");
            }

            String email = d.path("email").asText("");
            if (email.isBlank()) {
                try {
                    JsonNode emails = rest.get()
                        .uri("https://api.github.com/user/emails")
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .retrieve()
                        .body(JsonNode.class);

                    if (emails != null && emails.isArray()) {
                        for (JsonNode emailNode : emails) {
                            if (emailNode.path("primary").asBoolean() && emailNode.path("verified").asBoolean()) {
                                email = emailNode.path("email").asText("");
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            String name = d.path("name").asText("");
            if (name.isBlank()) {
                name = d.path("login").asText("");
            }

            return new SocialUserInfo(
                d.path("id").asText(),
                email,
                name,
                d.path("avatar_url").asText("")
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("GitHub user info fetch error", e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "GitHub 사용자 정보를 가져올 수 없습니다.");
        }
    }
}
