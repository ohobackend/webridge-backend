# WEBridge 백엔드 Java(Spring Boot) 전환 가이드

> 현재 Python(Django + DRF + Celery + Channels) 기반의 `webridge-backend`를 **Java 21 + Spring Boot 3.5** 기반으로 새로 만드는 방법을 정리한 문서입니다.
> 프론트엔드 수정 없이 넘어갈 수 있도록 **API 경로, JSON 형식, JWT, 비밀번호 해시, DB 스키마 호환성**을 지키는 것을 목표로 합니다.

---

## 목차

1. [현재 시스템 분석](#1-현재-시스템-분석)
2. [기술 스택 매핑 (Python → Java)](#2-기술-스택-매핑-python--java)
3. [목표 아키텍처](#3-목표-아키텍처)
4. [개발 환경 준비](#4-개발-환경-준비)
5. [프로젝트 생성](#5-프로젝트-생성)
6. [패키지 구조](#6-패키지-구조)
7. [설정 파일 (application.yml / .env)](#7-설정-파일-applicationyml--env)
8. [도메인별 구현 가이드](#8-도메인별-구현-가이드)
9. [반드시 지켜야 할 호환성 포인트](#9-반드시-지켜야-할-호환성-포인트)
10. [실행 방법](#10-실행-방법)
11. [단계별 전환 로드맵](#11-단계별-전환-로드맵)
12. [테스트 및 검증](#12-테스트-및-검증)
13. [배포](#13-배포)
14. [체크리스트](#14-체크리스트)

---

## 1. 현재 시스템 분석

### 1.1 구성 요소

| 구성 요소 | 현재 구현 | 역할 |
|---|---|---|
| 웹 프레임워크 | Django 5.2 + DRF 3.16 | REST API (`/api/v1/...`) |
| 인증 | `djangorestframework-simplejwt` (HS256, Access 6h / Refresh 30d, Rotation + Blacklist) | JWT 로그인, 토큰 갱신, 로그아웃 |
| 소셜 로그인 | `core/utils/social_auth.py` (Google / Kakao / GitHub 액세스 토큰으로 사용자 정보 조회) | 소셜 로그인 |
| 이메일 | Django SMTP + HTML 템플릿 (`templates/emails/*.html`) | 이메일 인증 링크, 비밀번호 재설정 |
| 비동기 작업 | Celery 5 + Redis (큐: `default`, `accessibility`, `reports`) | 접근성 검사, 리포트 생성, 중단된 검사 정리 |
| 실시간 진행률 | Django Channels WebSocket (`ws/scans/<uuid>/`) + SSE (`scans/<uuid>/stream/`) | 검사 진행률 푸시 |
| 접근성 검사 엔진 | `core/utils/accessibility_checker.py` (Playwright Chromium + `page.evaluate` JS + OpenAI GPT-4o) | 13개 항목 검사 |
| 리포트 | ReportLab(PDF), openpyxl(Excel), JSON | 리포트 생성/다운로드 |
| API 문서 | drf-spectacular (Swagger `/api/docs/`) | OpenAPI |
| 관리자 | Django Admin (`core/admin.py`, 약 100KB) | 운영자용 관리 화면 |
| DB | PostgreSQL (`DATABASE_URL`) / 로컬 SQLite | 데이터 저장 |
| 로깅 | Thread-local 컨텍스트 + RotatingFileHandler | Request ID / Task ID 로깅 |

### 1.2 데이터 모델 (테이블)

| 테이블 | Django 모델 | 비고 |
|---|---|---|
| `users` | `User` | email 로그인, provider(email/google/kakao/github), 약관 동의 필드, 추가 정보 |
| `terms` | `Terms` | 약관 종류(service/privacy/marketing)별 현재 버전 |
| `social_accounts` | `SocialAccount` | `(provider, provider_id)` unique, `extra_data` JSON |
| `email_verifications` | `EmailVerification` | 회원가입 전 이메일 인증 토큰 |
| `accessibility_scans` | `AccessibilityScan` | **UUID PK**, status, progress, 13개 `*_completed` 플래그 |
| `alt_text_results` 외 12개 | `AltTextResult`, `ContrastResult`, `KeyboardResult`, `LabelResult`, `TableResult`, `VideoCaptionResult`, `VideoAutoplayResult`, `BasicLanguageResult`, `MarkupErrorResult`, `HeadingResult`, `ResponseTimeResult`, `PauseControlResult`, `FlashingResult` | 검사 항목별 결과 (scan FK, CASCADE) |
| `alt_text_feedbacks` | `AltTextFeedback` | AI 제안 alt 텍스트 좋아요/싫어요 |
| `scan_reports` | `ScanReport` | scan과 1:1 |
| `feedbacks` | `Feedback` | 서비스 피드백 (별점) |
| `surveys` | `Survey` | **UUID PK**, 복수 선택 필드는 JSON 배열 |
| `survey_triggers` | `SurveyTrigger` | user와 1:1 |
| `token_blacklist_*` | simplejwt | Refresh 토큰 블랙리스트 |

### 1.3 API 엔드포인트 (모두 `/api/v1` 접두사)

| 그룹 | Method | 경로 | 인증 |
|---|---|---|---|
| Health | GET | `/ping/`, `/health/` | X |
| Auth | POST | `/auth/register/`, `/auth/login/`, `/auth/social-login/` | X |
| Auth | POST | `/auth/logout/`, `/auth/social-terms-agreement/`, `/auth/change-password/` | O |
| Auth | POST | `/auth/password-reset/`, `/auth/password-reset-confirm/`, `/auth/token/refresh/` | X |
| Auth | POST | `/auth/request-email-verification/`, `/auth/check-email-verification/` | X |
| Auth | GET | `/auth/verify-email-link/` (HTML 응답) | X |
| Terms | GET | `/terms/current/` | X |
| Terms | GET / POST | `/terms/status/`, `/terms/update/` | O |
| Scan | GET | `/scans/` (페이지네이션, 기본 10) | O |
| Scan | POST | `/scans/create/` | O |
| Scan | GET | `/scans/{uuid}/`, `/scans/{uuid}/progress/`, `/scans/{uuid}/download-report/` | O |
| Scan | DELETE | `/scans/{uuid}/delete/` | O |
| Scan | POST | `/scans/{uuid}/generate-report/`, `/scans/{uuid}/restart/` | O |
| Scan | GET | `/scans/{uuid}/stream/` (SSE) | O |
| Scan | GET | `/scans/{uuid}/alt-text-results/` | O |
| User | GET/PUT/PATCH/DELETE | `/user/me` (**끝 슬래시 없음**) | O |
| User | POST/PATCH | `/user/extra-info/` | O |
| Feedback | POST / GET | `/feedback/create/`, `/feedback/list/` (페이지네이션, 기본 20) | - |
| Feedback | GET / PUT / DELETE | `/feedback/{id}/`, `/feedback/{id}/update/`, `/feedback/{id}/delete/` | - |
| Survey | POST / GET | `/survey/create/`, `/survey/check-trigger/`, `/survey/mark-shown/` | O |
| AltText Feedback | POST / GET / DELETE | `/alt-text-feedback/create/`, `/alt-text-feedback/my/`, `/alt-text-feedback/result/{id}/`, `/alt-text-feedback/{id}/delete/` | O |
| WebSocket | WS | `/ws/scans/{uuid}/` | - |
| 문서 | GET | `/api/schema/`, `/api/docs/`, `/api/redoc/` | X |

> [!NOTE]
> 각 API의 상세 요청/응답은 이미 `docs/auth-api.md`, `docs/scan-api.md`, `docs/user-api.md`, `docs/feedback-api.md`, `docs/terms-api.md`, `docs/health-api.md`에 정리되어 있습니다. **Java 구현 시 이 문서들을 "계약서(Contract)"로 삼으세요.**

---

## 2. 기술 스택 매핑 (Python → Java)

| Python | Java (권장) | 비고 |
|---|---|---|
| Django / DRF | **Spring Boot 3.5 (Spring Web MVC)** | `@RestController` |
| Django ORM / migrations | **Spring Data JPA (Hibernate 6) + Flyway** | 스키마는 Flyway SQL로 관리 |
| DRF Serializer | **DTO(record) + Jakarta Bean Validation** | `@Valid`, `@NotBlank`, `@Email` 등 |
| simplejwt | **Spring Security + jjwt 0.12** | HS256, 같은 `SECRET_KEY`면 기존 토큰 호환 가능 |
| `token_blacklist` | Redis (`blacklist:{jti}` TTL) 또는 DB 테이블 | |
| Django 비밀번호 해시 (`pbkdf2_sha256$...`) | **커스텀 `PasswordEncoder`** | 기존 사용자 로그인 유지 (9.3 참조) |
| `requests` | Spring `RestClient` | 소셜 로그인, OpenAI 호출 |
| Celery + Redis | **`@Async` + `ThreadPoolTaskExecutor`** (1단계) → Redis Stream / RabbitMQ 워커 분리 (2단계) | |
| Celery Beat | **`@Scheduled` + ShedLock** | 다중 인스턴스 중복 실행 방지 |
| Django Channels (WebSocket) | **Spring WebSocket (raw `TextWebSocketHandler`)** + Redis Pub/Sub | 프론트가 raw WebSocket이므로 **STOMP 사용 금지** |
| `StreamingHttpResponse` (SSE) | **`SseEmitter`** | |
| Playwright (Python) | **Playwright for Java** (`com.microsoft.playwright:playwright`) | API가 거의 동일, JS 코드 그대로 재사용 |
| openai (Python SDK) | `RestClient`로 직접 호출 또는 공식 `openai-java` SDK | |
| ReportLab | **OpenPDF** (LGPL/MPL) | iText 7은 AGPL이므로 주의 |
| openpyxl | **Apache POI** (`poi-ooxml`) | |
| Django 템플릿 (이메일 HTML) | **Thymeleaf** | `{{ var }}` → `th:text="${var}"` |
| drf-spectacular | **springdoc-openapi** | `/swagger-ui.html` (경로 변경 가능) |
| django-cors-headers | Spring Security `CorsConfigurationSource` | |
| python-dotenv | `spring.config.import: optional:file:.env[.properties]` | |
| logging + thread-local filter | **SLF4J + Logback + MDC** | `requestId`, `taskId`, `userId` |
| Django Admin | (전환기) **Django Admin 유지** → (장기) 관리자 API + React-Admin 등 | 9.8 참조 |
| management commands | `CommandLineRunner` / 관리자 API / `@Scheduled` | |
| gunicorn / uvicorn | 내장 Tomcat | |

---

## 3. 목표 아키텍처

```mermaid
flowchart LR
    FE["Frontend (React)"] -->|REST /api/v1| API["Spring Boot API"]
    FE -->|"WebSocket /ws/scans/{id}/"| API
    FE -->|"SSE /scans/{id}/stream/"| API
    API --> PG[("PostgreSQL")]
    API --> RD[("Redis")]
    API -->|"@Async / Queue"| WK["Scan Worker (Playwright + OpenAI)"]
    WK --> PG
    WK -->|"진행률 Pub/Sub"| RD
    RD -->|"구독 후 WS/SSE 푸시"| API
    WK --> OAI["OpenAI API"]
    WK --> CH["Chromium (Playwright)"]
    API --> SMTP["SMTP (Gmail)"]
```

### 3.1 단계별 접근

- **1단계 (단일 애플리케이션)**: API와 검사 워커를 하나의 Spring Boot 앱에서 실행. 검사는 전용 `ThreadPoolTaskExecutor`(동시 1~2개)로 실행. 현재 Celery `solo` pool 설정(동시성 1)과 동일한 수준.
- **2단계 (워커 분리, 선택)**: 트래픽이 늘면 Gradle 멀티모듈로 `api` / `worker`를 분리하고 Redis Stream(또는 RabbitMQ)으로 작업 전달.

> [!TIP]
> 처음부터 멀티모듈/메시지 큐로 시작하지 마세요. 현재 Celery 설정도 `solo` pool + concurrency 1이므로, 1단계 구조로 충분히 동일한 성능을 낼 수 있습니다.

---

## 4. 개발 환경 준비

| 도구 | 버전 | 확인 명령 |
|---|---|---|
| JDK | **21 (LTS)** — 이 PC에 Temurin 21 설치 확인됨 | `java -version` |
| Gradle | 별도 설치 불필요 (Gradle Wrapper `gradlew` 사용) | |
| IDE | IntelliJ IDEA (Community 가능) 또는 VS Code + Extension Pack for Java | |
| Docker Desktop | 최신 | `docker -v` |
| PostgreSQL | 16 (Docker로 실행) | |
| Redis | 7 (Docker로 실행) | |

### 4.1 로컬 인프라 (docker-compose)

새 Java 프로젝트 루트에 `docker-compose.yml` 작성:

```yaml
services:
  postgres:
    image: postgres:16
    container_name: webridge-postgres
    environment:
      POSTGRES_DB: webridge
      POSTGRES_USER: webridge
      POSTGRES_PASSWORD: webridge
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7
    container_name: webridge-redis
    ports:
      - "6379:6379"

volumes:
  pgdata:
```

```powershell
docker compose up -d
docker ps   # postgres, redis 실행 확인
```

---

## 5. 프로젝트 생성

### 5.1 Spring Initializr로 생성

웹에서 https://start.spring.io 접속 후 아래처럼 선택하거나, PowerShell에서 바로 받을 수 있습니다.

- Project: **Gradle - Kotlin**, Language: **Java**, Spring Boot: **3.5.x**
- Group: `com.webridge`, Artifact: `webridge-backend`, Java: **21**
- Dependencies: Spring Web, Spring Data JPA, Spring Security, Validation, Java Mail Sender, Thymeleaf, WebSocket, Spring Data Redis, PostgreSQL Driver, Flyway Migration, Lombok, Spring Boot Actuator

```powershell
cd C:\dev
curl.exe -G https://start.spring.io/starter.zip `
  -d type=gradle-project-kotlin -d language=java -d javaVersion=21 `
  -d groupId=com.webridge -d artifactId=webridge-backend -d name=webridge-backend `
  -d packageName=com.webridge `
  -d dependencies=web,data-jpa,security,validation,mail,thymeleaf,websocket,data-redis,postgresql,flyway,lombok,actuator `
  -o webridge-backend-java.zip
Expand-Archive webridge-backend-java.zip -DestinationPath webridge-backend-java
cd webridge-backend-java
```

> [!IMPORTANT]
> 기존 Python 저장소(`webridge-backend-main`)와 **별도 폴더/저장소**로 만드세요. 전환 기간 동안 두 서버를 병행 운영하며 응답을 비교해야 합니다.

### 5.2 `build.gradle.kts`

Initializr가 만든 파일에 아래 의존성을 추가합니다.

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.webridge"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
}

repositories { mavenCentral() }

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // DB
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // API 문서 (Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    // 접근성 검사 (Python과 같은 1.53 계열 권장)
    implementation("com.microsoft.playwright:playwright:1.53.0")

    // 리포트
    implementation("com.github.librepdf:openpdf:2.0.3")
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    // 스케줄 중복 실행 방지
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-redis-spring:5.16.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }

// Playwright 브라우저(Chromium) 설치 태스크: ./gradlew playwrightInstall
tasks.register<JavaExec>("playwrightInstall") {
    group = "setup"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.microsoft.playwright.CLI")
    args = listOf("install", "chromium")
}
```

> [!NOTE]
> 버전 번호는 작성 시점 기준입니다. 실제 생성 시 Initializr가 제안하는 최신 패치 버전을 사용하고, springdoc은 Spring Boot 버전과 호환되는 버전을 선택하세요.

---

## 6. 패키지 구조

Django의 `core` 단일 앱을 **도메인(기능) 단위 패키지**로 나눕니다.

```text
webridge-backend-java/
├─ build.gradle.kts
├─ docker-compose.yml
├─ .env                                  # Git에 올리지 말 것
├─ src/main/java/com/webridge/
│  ├─ WebridgeApplication.java
│  ├─ config/
│  │  ├─ SecurityConfig.java             # JWT 필터, CORS, 권한
│  │  ├─ AsyncConfig.java                # 검사/리포트용 Executor (Celery 대체)
│  │  ├─ SchedulingConfig.java           # @EnableScheduling + ShedLock
│  │  ├─ WebSocketConfig.java            # /ws/scans/{id}/
│  │  ├─ RedisConfig.java                # Pub/Sub, 블랙리스트
│  │  ├─ JacksonConfig.java              # snake_case, 날짜 포맷
│  │  ├─ WebConfig.java                  # 트레일링 슬래시 처리
│  │  └─ OpenApiConfig.java              # Swagger 태그/보안 스키마
│  ├─ common/
│  │  ├─ exception/                      # GlobalExceptionHandler, ApiException
│  │  ├─ logging/                        # MdcLoggingFilter (LoggingContextMiddleware 대체)
│  │  └─ dto/PageResponse.java           # DRF 페이지네이션 형식
│  ├─ auth/
│  │  ├─ AuthController.java             # auth_views.py
│  │  ├─ AuthService.java
│  │  ├─ EmailVerificationService.java
│  │  ├─ PasswordResetService.java
│  │  ├─ jwt/ (JwtProvider, JwtAuthenticationFilter, TokenBlacklistService)
│  │  ├─ password/DjangoPbkdf2PasswordEncoder.java
│  │  ├─ social/ (SocialUserInfoClient: Google/Kakao/GitHub)
│  │  ├─ mail/MailService.java
│  │  └─ dto/
│  ├─ user/       (User, UserRepository, UserController, UserService, dto)
│  ├─ terms/      (Terms, TermsRepository, TermsController, TermsService)
│  ├─ scan/
│  │  ├─ ScanController.java             # scan_views.py
│  │  ├─ ScanService.java
│  │  ├─ ScanTaskRunner.java             # tasks.py run_accessibility_scan
│  │  ├─ ScanCleanupScheduler.java       # cleanup_stuck_scans
│  │  ├─ progress/ (ScanProgressPublisher, ScanWebSocketHandler, ScanSseService)
│  │  ├─ entity/   (AccessibilityScan, AltTextResult, ... 13개 결과, ScanReport)
│  │  ├─ repository/
│  │  ├─ checker/
│  │  │  ├─ AccessibilityChecker.java    # accessibility_checker.py
│  │  │  ├─ BrowserFactory.java
│  │  │  ├─ AltTextAiClient.java         # OpenAI 호출
│  │  │  └─ ContrastCalculator.java
│  │  ├─ report/ (PdfReportGenerator, ExcelReportGenerator, JsonReportGenerator)
│  │  └─ dto/
│  ├─ feedback/   (Feedback ...)
│  ├─ survey/     (Survey, SurveyTrigger ...)
│  ├─ alttextfeedback/
│  └─ health/     (HealthController)
├─ src/main/resources/
│  ├─ application.yml
│  ├─ application-local.yml
│  ├─ application-prod.yml
│  ├─ logback-spring.xml
│  ├─ db/migration/                      # Flyway: V1__init.sql ...
│  ├─ templates/
│  │  ├─ emails/verification_link.html   # Thymeleaf로 변환
│  │  ├─ emails/password_reset.html
│  │  └─ auth/verification_result.html
│  ├─ checker-js/                        # page.evaluate()에 쓰던 JS를 파일로 분리
│  │  ├─ contrast_styles.js
│  │  ├─ markup_duplicate_ids.js
│  │  ├─ headings_page_title.js
│  │  ├─ pause_control_scan.js
│  │  └─ flashing.js ...
│  └─ static/webridge_logo.png
└─ src/test/java/com/webridge/...
```

---

## 7. 설정 파일 (application.yml / .env)

### 7.1 `.env` (기존 Python `.env`와 같은 키 유지)

```properties
SECRET_KEY=기존_Django_SECRET_KEY_그대로
DEBUG=True
DATABASE_URL_JDBC=jdbc:postgresql://localhost:5432/webridge
DB_USERNAME=webridge
DB_PASSWORD=webridge
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_ACCESS_TOKEN_LIFETIME=6
JWT_REFRESH_TOKEN_LIFETIME=30
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_HOST_USER=
EMAIL_HOST_PASSWORD=
FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://127.0.0.1:8080
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=
OPENAI_API_KEY=
```

> [!WARNING]
> Python은 `DATABASE_URL=postgres://user:pass@host:5432/db` 형식이지만 JDBC는 `jdbc:postgresql://host:5432/db` + 별도 username/password가 필요합니다. 배포 플랫폼(Railway 등)에서 변수를 따로 만들어 주세요.

### 7.2 `application.yml`

```yaml
spring:
  config:
    import: optional:file:.env[.properties]   # .env 자동 로드 (python-dotenv 대체)
  application:
    name: webridge-backend

  datasource:
    url: ${DATABASE_URL_JDBC}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate          # 스키마는 Flyway가 관리, JPA는 검증만
    properties:
      hibernate:
        jdbc.time_zone: UTC
        format_sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true     # 기존 Django DB에 붙일 때 필요
    baseline-version: 1

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

  jackson:
    property-naming-strategy: SNAKE_CASE   # DRF 응답과 동일한 snake_case
    time-zone: Asia/Seoul
    serialization:
      write-dates-as-timestamps: false

  mail:
    host: ${EMAIL_HOST:smtp.gmail.com}
    port: ${EMAIL_PORT:587}
    username: ${EMAIL_HOST_USER:}
    password: ${EMAIL_HOST_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

  servlet:
    multipart:
      max-file-size: 10MB

server:
  port: 8080
  forward-headers-strategy: framework

springdoc:
  api-docs.path: /api/schema
  swagger-ui.path: /api/docs

webridge:
  jwt:
    secret: ${SECRET_KEY}
    access-token-hours: ${JWT_ACCESS_TOKEN_LIFETIME:6}
    refresh-token-days: ${JWT_REFRESH_TOKEN_LIFETIME:30}
  cors-allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
  backend-url: ${BACKEND_URL:http://127.0.0.1:8080}
  openai-api-key: ${OPENAI_API_KEY:}
  reports-dir: ./reports
  scan:
    executor-pool-size: 1          # Celery solo/concurrency=1 과 동일
    task-timeout-seconds: 900      # Celery time_limit=900
  social:
    google: { client-id: "${GOOGLE_CLIENT_ID:}", client-secret: "${GOOGLE_CLIENT_SECRET:}" }
    kakao:  { client-id: "${KAKAO_CLIENT_ID:}",  client-secret: "${KAKAO_CLIENT_SECRET:}" }
    github: { client-id: "${GITHUB_CLIENT_ID:}", client-secret: "${GITHUB_CLIENT_SECRET:}" }
```

설정값은 `@ConfigurationProperties(prefix = "webridge")` 레코드로 바인딩해서 사용합니다.

---

## 8. 도메인별 구현 가이드

### 8.1 Entity (Django Model → JPA)

**필드 매핑 규칙**

| Django | JPA / Java |
|---|---|
| `BigAutoField` (기본 PK) | `@Id @GeneratedValue(strategy = IDENTITY) Long id` |
| `UUIDField(primary_key, default=uuid4)` | `@Id UUID id` + `@PrePersist`에서 `UUID.randomUUID()` |
| `CharField(max_length=n)` | `@Column(length = n) String` |
| `TextField` | `@Column(columnDefinition = "text") String` |
| `BooleanField` | `boolean` / `Boolean` |
| `IntegerField` / `FloatField` | `int` / `double` |
| `DateTimeField(auto_now_add)` | `@CreationTimestamp OffsetDateTime createdAt` |
| `DateTimeField(auto_now)` | `@UpdateTimestamp OffsetDateTime updatedAt` |
| `JSONField` (Postgres `jsonb`) | `@JdbcTypeCode(SqlTypes.JSON) List<String>` / `Map<String,Object>` |
| `ForeignKey(on_delete=CASCADE)` | `@ManyToOne(fetch = LAZY)` + DB FK `ON DELETE CASCADE` |
| `ForeignKey(on_delete=SET_NULL)` | `@ManyToOne` + DB FK `ON DELETE SET NULL` |
| `OneToOneField` | `@OneToOne` |
| `choices=[...]` | `enum` + `AttributeConverter` (DB에는 기존 소문자 문자열 저장) |
| `save()` 오버라이드 (`HeadingResult.total_issues`) | `@PrePersist` / `@PreUpdate` |
| `@property` | 일반 getter 메서드 |

**예시: `AccessibilityScan`**

```java
@Entity
@Table(name = "accessibility_scans")
@Getter @Setter
public class AccessibilityScan {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "text", nullable = false)
    private String url;

    @Column(length = 200, nullable = false)
    private String title = "";

    @Convert(converter = ScanStatusConverter.class)
    @Column(length = 20, nullable = false)
    private ScanStatus status = ScanStatus.PENDING;

    private int progress;
    private int totalIssues;
    private double complianceScore;

    private boolean altTextCompleted;
    private boolean contrastCompleted;
    private boolean keyboardCompleted;
    private boolean labelCompleted;
    private boolean tableCompleted;
    private boolean videoCaptionCompleted;
    private boolean videoAutoplayCompleted;
    private boolean basicLanguageCompleted;
    private boolean markupErrorCompleted;
    private boolean headingCompleted;
    private boolean responseTimeCompleted;
    private boolean pauseControlCompleted;
    private boolean flashingCompleted;

    @CreationTimestamp private OffsetDateTime createdAt;
    @UpdateTimestamp   private OffsetDateTime updatedAt;
    private OffsetDateTime completedAt;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @OneToMany(mappedBy = "scan", cascade = CascadeType.REMOVE)
    private List<AltTextResult> altTextResults = new ArrayList<>();
    // ... 나머지 결과 컬렉션

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public void resetForRun() {
        status = ScanStatus.PROCESSING; progress = 0; errorMessage = "";
        totalIssues = 0; complianceScore = 0.0; completedAt = null;
        altTextCompleted = contrastCompleted = keyboardCompleted = labelCompleted =
        tableCompleted = videoCaptionCompleted = videoAutoplayCompleted =
        basicLanguageCompleted = markupErrorCompleted = headingCompleted =
        responseTimeCompleted = pauseControlCompleted = flashingCompleted = false;
    }
}
```

```java
public enum ScanStatus {
    PENDING("pending"), PROCESSING("processing"), COMPLETED("completed"),
    FAILED("failed"), CANCELLED("cancelled");   // tasks.py에서 'cancelled'를 사용하므로 반드시 포함
    private final String value;
    ScanStatus(String v) { this.value = v; }
    @JsonValue public String getValue() { return value; }
    public static ScanStatus from(String v) {
        return Arrays.stream(values()).filter(s -> s.value.equals(v)).findFirst().orElseThrow();
    }
}

@Converter
public class ScanStatusConverter implements AttributeConverter<ScanStatus, String> {
    public String convertToDatabaseColumn(ScanStatus s) { return s == null ? null : s.getValue(); }
    public ScanStatus convertToEntityAttribute(String v) { return v == null ? null : ScanStatus.from(v); }
}
```

> [!WARNING]
> 현재 Django 모델의 `STATUS_CHOICES`에는 `cancelled`가 없지만 `tasks.py`에서 `scan.status = 'cancelled'`를 저장합니다. Java enum에 `CANCELLED`가 없으면 기존 데이터 조회 시 예외가 납니다.

**예시: `User`** — Django `AbstractBaseUser` / `PermissionsMixin`이 만든 컬럼(`password`, `last_login`, `is_superuser`)도 매핑해야 합니다.

```java
@Entity
@Table(name = "users")
@Getter @Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false) private String password;   // Django 해시 형식
    private OffsetDateTime lastLogin;
    private boolean isSuperuser;

    @Column(length = 255, unique = true, nullable = false) private String email;
    @Column(length = 100, nullable = false) private String name;

    @Column(length = 20, nullable = false) private String provider = "email";
    @Column(length = 255) private String providerId;

    private boolean isActive = true;
    private boolean isAdmin;

    private boolean serviceTermsAgreed;
    private boolean ageVerificationAgreed;
    private boolean privacyPolicyAgreed;
    private boolean marketingAgreed;
    @Column(length = 10) private String serviceTermsVersion = "1.0";
    @Column(length = 10) private String privacyPolicyVersion = "1.0";
    @Column(length = 10) private String marketingVersion = "1.0";
    private OffsetDateTime serviceTermsAgreedAt;
    private OffsetDateTime ageVerificationAgreedAt;
    private OffsetDateTime privacyPolicyAgreedAt;
    private OffsetDateTime marketingAgreedAt;

    private boolean isSocialSignupComplete;

    @Column(length = 1)   private String pathToWebridge;
    @Column(length = 100) private String pathToWebridgeOther;
    @Column(length = 1)   private String occupation;
    @Column(length = 100) private String occupationOther;

    @CreationTimestamp private OffsetDateTime createdAt;
    @UpdateTimestamp   private OffsetDateTime updatedAt;

    public boolean hasRequiredAgreements() {
        return serviceTermsAgreed && ageVerificationAgreed && privacyPolicyAgreed;
    }
    public boolean needsSocialTermsAgreement() {
        if ("email".equals(provider)) return false;
        return !isSocialSignupComplete || !hasRequiredAgreements();
    }
}
```

> [!NOTE]
> `needs_terms_update()`, `get_outdated_terms()`, `update_terms_agreement()`처럼 다른 모델(`Terms`)을 조회하는 메서드는 Entity가 아니라 **`TermsService`**로 옮기세요. JPA Entity 안에서 Repository를 호출하지 않는 것이 Spring의 일반적인 방식입니다.

### 8.2 Repository

```java
public interface AccessibilityScanRepository extends JpaRepository<AccessibilityScan, UUID> {
    Page<AccessibilityScan> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<AccessibilityScan> findByIdAndUserId(UUID id, Long userId);
    List<AccessibilityScan> findByStatusInAndUpdatedAtBefore(List<ScanStatus> s, OffsetDateTime t);
}

public interface TermsRepository extends JpaRepository<Terms, Long> {
    Optional<Terms> findByTypeAndIsActiveTrue(String type);   // Terms.get_current_terms()
}
```

### 8.3 인증 (JWT / Security)

**SecurityConfig**

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final WebridgeProperties props;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/ping/**", "/api/v1/health/**").permitAll()
                .requestMatchers("/api/v1/auth/register/**", "/api/v1/auth/login/**",
                        "/api/v1/auth/social-login/**", "/api/v1/auth/password-reset/**",
                        "/api/v1/auth/password-reset-confirm/**", "/api/v1/auth/token/refresh/**",
                        "/api/v1/auth/request-email-verification/**",
                        "/api/v1/auth/verify-email-link/**",
                        "/api/v1/auth/check-email-verification/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/terms/current/**").permitAll()
                .requestMatchers("/api/schema/**", "/api/docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/v1/feedback/**").permitAll()   // 기존 권한 확인 후 조정
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(
                (req, res, ex) -> {   // DRF 401 응답 형식 유지
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"detail\":\"자격 인증데이터(authentication credentials)가 제공되지 않았습니다.\"}");
                }))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(props.corsAllowedOrigins().split(",")));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // 신규 해시도 Django 형식으로 저장하면 전환기에 Django Admin과도 호환됨
        return new DjangoPbkdf2PasswordEncoder();
    }
}
```

> [!IMPORTANT]
> 각 엔드포인트의 기존 권한(`AllowAny` / `IsAuthenticated`)을 `core/views/*.py`의 `@permission_classes`에서 하나씩 확인해 그대로 옮기세요. 위 예시의 feedback 권한은 반드시 실제 코드와 대조해야 합니다.

**JwtProvider (simplejwt 호환)**

simplejwt 토큰 payload 예시:

```json
{ "token_type": "access", "exp": 1730000000, "iat": 1729978400, "jti": "abc...", "user_id": 1 }
```

```java
@Component
public class JwtProvider {
    private final SecretKey key;
    private final Duration accessTtl, refreshTtl;

    public JwtProvider(WebridgeProperties p) {
        // simplejwt는 SECRET_KEY 문자열의 UTF-8 바이트를 HS256 키로 사용
        this.key = new SecretKeySpec(p.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.accessTtl = Duration.ofHours(p.jwt().accessTokenHours());
        this.refreshTtl = Duration.ofDays(p.jwt().refreshTokenDays());
    }

    public String createAccess(Long userId)  { return create(userId, "access", accessTtl, Map.of()); }
    public String createRefresh(Long userId) { return create(userId, "refresh", refreshTtl, Map.of()); }

    public String create(Long userId, String type, Duration ttl, Map<String, Object> extra) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("token_type", type)
                .claim("user_id", userId)
                .claims(extra)
                .id(UUID.randomUUID().toString().replace("-", ""))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
```

> [!TIP]
> Django의 `SECRET_KEY`를 그대로 쓰고 claim 이름(`user_id`, `token_type`, `jti`)을 똑같이 맞추면, **전환 시점에 이미 로그인된 사용자의 토큰이 Java 서버에서도 그대로 유효**합니다. Django의 `SECRET_KEY`가 32바이트 이상인지 확인하세요(jjwt는 HS256 키가 256비트 미만이면 거부합니다).

**토큰 갱신 / 로그아웃 (Rotation + Blacklist)**

- `/auth/token/refresh/`: refresh 검증 → 블랙리스트 확인 → **기존 refresh의 jti를 블랙리스트 등록** → 새 access + 새 refresh 발급 (`ROTATE_REFRESH_TOKENS`, `BLACKLIST_AFTER_ROTATION`과 동일)
- `/auth/logout/`: 요청 body의 refresh jti를 블랙리스트 등록
- 블랙리스트는 Redis에 `jwt:blacklist:{jti}` 키를 **남은 만료 시간만큼 TTL**로 저장

```java
@Service @RequiredArgsConstructor
public class TokenBlacklistService {
    private final StringRedisTemplate redis;
    public void blacklist(String jti, Date exp) {
        long ttl = exp.getTime() - System.currentTimeMillis();
        if (ttl > 0) redis.opsForValue().set("jwt:blacklist:" + jti, "1", Duration.ofMillis(ttl));
    }
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey("jwt:blacklist:" + jti));
    }
}
```

**비밀번호 재설정 토큰**: 기존 `PasswordResetToken`은 `token_type=password_reset`, 30분 만료, `email`, `purpose` claim을 가진 JWT입니다. `jwtProvider.create(userId, "password_reset", Duration.ofMinutes(30), Map.of("email", email, "purpose", "password_reset"))`로 동일하게 만듭니다.

### 8.4 소셜 로그인

`social_auth.py`를 `RestClient`로 그대로 옮깁니다.

```java
@Component
public class SocialUserInfoClient {
    private final RestClient rest = RestClient.create();

    public SocialUserInfo fetch(String provider, String accessToken) {
        return switch (provider) {
            case "google" -> google(accessToken);
            case "kakao"  -> kakao(accessToken);
            case "github" -> github(accessToken);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 제공자: " + provider);
        };
    }

    private SocialUserInfo google(String token) {
        JsonNode d = rest.get().uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .header("Authorization", "Bearer " + token).retrieve().body(JsonNode.class);
        return new SocialUserInfo(d.path("id").asText(), d.path("email").asText(),
                d.path("name").asText(""), d.path("picture").asText(""));
    }

    private SocialUserInfo kakao(String token) {
        JsonNode d = rest.get().uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + token).retrieve().body(JsonNode.class);
        JsonNode acc = d.path("kakao_account"), profile = acc.path("profile");
        return new SocialUserInfo(d.path("id").asText(), acc.path("email").asText(""),
                profile.path("nickname").asText(""), profile.path("profile_image_url").asText(""));
    }

    private SocialUserInfo github(String token) {
        // /user 호출 후 email이 없으면 /user/emails 에서 primary && verified 이메일 선택
        // (social_auth.py get_github_user_info 로직 그대로)
        ...
    }
}
```

### 8.5 이메일 (Thymeleaf)

1. `core/templates/emails/*.html` → `src/main/resources/templates/emails/`로 복사
2. Django 문법을 Thymeleaf로 변환: `{{ verification_url }}` → `<a th:href="${verificationUrl}">`, `{% if %}` → `th:if`
3. `MimeMessageHelper`로 HTML 메일 발송 (`EmailMultiAlternatives` 대체)

```java
@Service @RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async("mailExecutor")
    public void sendHtml(String to, String subject, String template, Map<String, Object> vars) {
        try {
            String html = templateEngine.process(template, new Context(Locale.KOREAN, vars));
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setTo(to); h.setSubject(subject); h.setText(html, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new IllegalStateException("메일 발송 실패", e);
        }
    }
}
```

### 8.6 비동기 작업 (Celery 대체)

**AsyncConfig**

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("scanExecutor")   // queue: accessibility
    ThreadPoolTaskExecutor scanExecutor(WebridgeProperties p) {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(p.scan().executorPoolSize());
        ex.setMaxPoolSize(p.scan().executorPoolSize());
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("scan-");
        ex.setTaskDecorator(new MdcTaskDecorator());   // 로그 컨텍스트 전파
        ex.initialize();
        return ex;
    }

    @Bean("reportExecutor") // queue: reports
    ThreadPoolTaskExecutor reportExecutor() { /* core 2 */ ... }

    @Bean("mailExecutor")
    ThreadPoolTaskExecutor mailExecutor() { /* core 2 */ ... }
}
```

**ScanTaskRunner (tasks.py `run_accessibility_scan` 대체)**

```java
@Service @RequiredArgsConstructor @Slf4j
public class ScanTaskRunner {
    private final AccessibilityScanRepository scanRepo;
    private final ScanResultSaver resultSaver;          // 각 결과 저장 (트랜잭션 단위)
    private final ScanProgressPublisher progress;
    private final AccessibilityCheckerFactory checkerFactory;
    private final SurveyTriggerService surveyTriggerService;

    @Async("scanExecutor")
    public void run(UUID scanId) {
        MDC.put("taskId", "scan-" + scanId);
        AccessibilityScan scan = scanRepo.findById(scanId).orElseThrow();
        if (!scan.getUser().isActive()) { resultSaver.cancel(scanId, "사용자가 삭제되었거나 비활성화되었습니다."); return; }

        resultSaver.resetForRun(scanId);
        try (AccessibilityChecker checker = checkerFactory.create(scan.getUrl())) {
            resultSaver.saveTitleIfEmpty(scanId, checker.extractPageTitle());

            progress.update(scanId, 10, "이미지 대체 텍스트 검사 시작", "이미지 대체 텍스트 검사");
            resultSaver.saveAltText(scanId, checker.checkAltText());

            progress.update(scanId, 20, "색상 대비 검사 시작", "색상 대비 검사");
            resultSaver.saveContrast(scanId, checker.checkContrast());
            // ... keyboard, labels, tables, video caption/autoplay, basic language,
            //     markup errors, headings, response time, pause control, flashing
            //     (tasks.py의 진행률 숫자/메시지를 그대로 옮길 것)

            resultSaver.complete(scanId);   // total_issues, compliance_score 계산 후 completed
            surveyTriggerService.onScanCompleted(scanId);
            progress.update(scanId, 100, "검사가 완료되었습니다.", null);
        } catch (Exception e) {
            log.error("scan failed {}", scanId, e);
            resultSaver.fail(scanId, e.getMessage());
            progress.update(scanId, -1, "검사 중 오류가 발생했습니다.", null);
        } finally {
            MDC.clear();
        }
    }
}
```

> [!IMPORTANT]
> - `@Async` 메서드는 **다른 빈에서 호출**해야 프록시가 동작합니다(같은 클래스 내부 호출 X).
> - 컨트롤러에서는 `scanRepo.save()` **트랜잭션이 커밋된 뒤** `run()`을 호출해야 합니다. `@TransactionalEventListener(phase = AFTER_COMMIT)`를 쓰면 안전합니다.
> - 검사 한 번이 수 분 걸리므로 **전체를 하나의 `@Transactional`로 묶지 마세요.** 진행률/결과 저장은 짧은 트랜잭션으로 나눕니다.
> - Celery의 `time_limit=900`은 `CompletableFuture.orTimeout()` 또는 Playwright 타임아웃 + 감시 스케줄러로 구현합니다.

**재시도(`max_retries=2`)**: 필요하면 `spring-retry`의 `@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 30000))`를 사용합니다.

**중단된 검사 정리 (Celery Beat 대체)**

```java
@Component @RequiredArgsConstructor
public class ScanCleanupScheduler {
    private final ScanCleanupService cleanupService;

    @Scheduled(fixedDelayString = "PT30M")   // setup_periodic_cleanup 기본 30분
    @SchedulerLock(name = "cleanup-stuck-scans", lockAtMostFor = "PT10M")
    public void cleanupStuckScans() {
        cleanupService.cleanup();   // tasks.cleanup_stuck_scans 로직 이식
    }
}
```

> [!NOTE]
> 서버 재시작 시 `@Async` 큐의 대기 작업은 사라집니다(Celery는 Redis에 남음). DB에서 `pending`/`processing` 상태인 검사를 위 정리 작업이 `failed` 처리하거나, 앱 시작 시(`ApplicationReadyEvent`) `pending` 검사를 다시 큐에 넣는 로직을 추가하세요. 이 문제가 중요해지면 2단계(Redis Stream 워커)로 넘어갑니다.

### 8.7 실시간 진행률 (WebSocket + SSE)

**WebSocket (Channels `ScanProgressConsumer` 대체)** — 프론트가 raw WebSocket을 사용하므로 STOMP 없이 구현합니다.

```java
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final ScanWebSocketHandler handler;
    private final WebridgeProperties props;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/scans/{scanId}", "/ws/scans/{scanId}/")
                .setAllowedOrigins(props.corsAllowedOrigins().split(","));
    }
}
```

```java
@Component @RequiredArgsConstructor
public class ScanWebSocketHandler extends TextWebSocketHandler {
    private final Map<UUID, Set<WebSocketSession>> groups = new ConcurrentHashMap<>();
    private final AccessibilityScanRepository scanRepo;
    private final ObjectMapper om;

    @Override
    public void afterConnectionEstablished(WebSocketSession s) throws Exception {
        UUID scanId = extractScanId(s.getUri());          // /ws/scans/{uuid}/
        var scan = scanRepo.findById(scanId);
        if (scan.isEmpty()) { s.close(CloseStatus.POLICY_VIOLATION); return; }
        groups.computeIfAbsent(scanId, k -> ConcurrentHashMap.newKeySet()).add(s);
        send(s, Map.of("type", "scan_progress", "data", Map.of(
                "scan_id", scanId.toString(),
                "status", scan.get().getStatus().getValue(),
                "progress", scan.get().getProgress(),
                "message", scan.get().getStatus().getValue() + " - " + scan.get().getProgress() + "%")));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {
        groups.values().forEach(set -> set.remove(s));
    }

    /** Redis Pub/Sub 수신 시 호출 (group_send 대체) */
    public void broadcast(UUID scanId, Map<String, Object> data) {
        for (var s : groups.getOrDefault(scanId, Set.of())) {
            send(s, Map.of("type", "scan_progress", "data", data));
        }
    }
    // send(): synchronized(s) { s.sendMessage(new TextMessage(om.writeValueAsString(msg))); }
}
```

**ScanProgressPublisher**: 진행률 DB 저장 → Redis 채널 `scan-progress`에 publish → 모든 API 인스턴스가 구독해서 `ScanWebSocketHandler.broadcast()` + SSE emitter에 전달. (서버가 1대뿐이라면 Redis 없이 `ApplicationEventPublisher`로 시작해도 됩니다.)

**SSE (`scan_stream` 대체)**

```java
@GetMapping(value = "/scans/{scanId}/stream/", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@PathVariable UUID scanId, @AuthenticationPrincipal AuthUser me,
                         HttpServletResponse res) {
    res.setHeader("Cache-Control", "no-cache");
    res.setHeader("X-Accel-Buffering", "no");   // Nginx 버퍼링 비활성화
    return scanSseService.subscribe(scanId, me.id());   // 타임아웃 5분 (기존 max_iterations=300)
}
```

`ScanSseService`는 연결 직후 `type: initial` 이벤트를 보내고, 이미 `completed`/`failed`면 `final: true` 이벤트를 보낸 뒤 `emitter.complete()`합니다. 기존 Python처럼 1초 polling을 해도 되지만, Publisher 이벤트로 push하는 편이 DB 부하가 적습니다. **이벤트 JSON 필드(`scan_id`, `status`, `progress`, `current_test`, `message`, `timestamp`, `type`, `final`)는 기존과 동일하게** 유지하세요.

### 8.8 접근성 검사 엔진 (가장 큰 작업)

`accessibility_checker.py`는 약 2,300줄(100KB)로 전체 이식 작업의 핵심입니다. 다행히 **검사 로직의 대부분이 `page.evaluate('''JS 코드''')` 안의 JavaScript**라서, JS는 그대로 재사용하고 Python 래퍼만 Java로 옮기면 됩니다.

**1) JS 코드 분리**

Python 파일의 `page.evaluate('''...''')` 블록을 `src/main/resources/checker-js/*.js`로 각각 복사합니다.

```java
@Component
public class CheckerScripts {
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    public String get(String name) {
        return cache.computeIfAbsent(name, n -> {
            try (var in = new ClassPathResource("checker-js/" + n + ".js").getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) { throw new UncheckedIOException(e); }
        });
    }
}
```

**2) Playwright Java로 브라우저 제어**

```java
public class AccessibilityChecker implements AutoCloseable {
    private final Playwright playwright;
    private final Browser browser;
    private final Page page;
    private final CheckerScripts scripts;
    private final AltTextAiClient ai;

    public AccessibilityChecker(String url, CheckerScripts scripts, AltTextAiClient ai) {
        this.scripts = scripts; this.ai = ai;
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu")));
        this.page = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)).newPage();
        page.onConsoleMessage(m -> log.debug("[console] {}", m.text()));
        page.navigate(url, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60_000));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkMarkupDuplicateIds() {
        return (Map<String, Object>) page.evaluate(scripts.get("markup_duplicate_ids"));
    }

    @Override
    public void close() {   // _close_browser_safely
        try { browser.close(); } catch (Exception ignored) {}
        try { playwright.close(); } catch (Exception ignored) {}
    }
}
```

> [!WARNING]
> **Playwright Java 객체는 스레드 안전하지 않습니다.** `Playwright` 인스턴스는 생성한 스레드에서만 사용해야 합니다. Python 코드의 `_run_in_thread`, `_capture_image_in_thread`처럼 별도 스레드에서 브라우저를 쓰는 부분은, **그 스레드 안에서 `Playwright.create()`를 새로 호출**하는 방식으로 옮기세요. 검사 1건 = Playwright 1개 = 스레드 1개 원칙을 지키면 안전합니다.

**Python → Java Playwright API 대응**

| Python | Java |
|---|---|
| `sync_playwright().start()` | `Playwright.create()` |
| `p.chromium.launch(headless=True, args=[...])` | `p.chromium().launch(new LaunchOptions().setHeadless(true).setArgs(...))` |
| `page.goto(url, wait_until=..., timeout=...)` | `page.navigate(url, new NavigateOptions()...)` |
| `page.evaluate(js)` / `page.evaluate(js, arg)` | `page.evaluate(js)` / `page.evaluate(js, arg)` → `Object`(Map/List/Number/String) |
| `page.query_selector_all("img")` | `page.querySelectorAll("img")` 또는 `page.locator("img").all()` |
| `el.get_attribute("alt")` | `el.getAttribute("alt")` |
| `el.evaluate("e => e.outerHTML")` | `el.evaluate("e => e.outerHTML")` |
| `page.screenshot()` / `el.screenshot()` | `page.screenshot()` / `el.screenshot()` → `byte[]` |
| `page.on("console", fn)` | `page.onConsoleMessage(fn)` |
| `page.on("requestfailed", fn)` | `page.onRequestFailed(fn)` |
| `page.keyboard.press("Tab")` | `page.keyboard().press("Tab")` |
| `page.wait_for_timeout(ms)` | `page.waitForTimeout(ms)` |
| `time.sleep()` | `Thread.sleep()` 또는 `page.waitForTimeout()` |

**3) OpenAI 호출 (alt 텍스트 생성/평가)**

```java
@Component @RequiredArgsConstructor
public class AltTextAiClient {
    private final WebridgeProperties props;
    private final RestClient rest = RestClient.builder()
            .baseUrl("https://api.openai.com/v1").build();
    private final Semaphore rateLimiter = new Semaphore(5);   // Python Semaphore(5)

    public JsonNode chat(String systemPrompt, String userText, String imageDataUrl) {
        if (props.openaiApiKey().isBlank()) return null;
        var body = Map.of(
            "model", "gpt-4o-2024-08-06",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", List.of(
                    Map.of("type", "text", "text", userText),
                    Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))))));
        rateLimiter.acquireUninterruptibly();
        try {
            return rest.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.openaiApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(JsonNode.class);
        } finally { rateLimiter.release(); }
    }
}
```

> [!IMPORTANT]
> `_generate_alt_text_with_ai`, `_evaluate_alt_text_with_ai`의 **프롬프트 문자열과 응답 파싱 로직은 한 글자도 바꾸지 말고** 그대로 옮기세요(`resources/prompts/*.txt`로 분리 권장). 프롬프트가 달라지면 AI 등급 결과가 달라집니다.

**4) 이미지 병렬 처리**

Python의 `ThreadPoolExecutor` + `as_completed`는 Java에서 `ExecutorService` + `CompletableFuture` (또는 Java 21 **가상 스레드** `Executors.newVirtualThreadPerTaskExecutor()`)로 옮깁니다. OpenAI 호출처럼 I/O 위주의 작업은 가상 스레드가 잘 맞습니다. 단, Playwright 객체는 여러 스레드에서 공유하지 마세요.

**5) 색상 대비 계산** (`_calculate_contrast_ratio`) — 순수 계산이므로 `ContrastCalculator`로 옮기고 **단위 테스트를 꼭 작성**하세요(Python 결과와 같은 값이 나오는지 비교).

**6) 이식 순서 (쉬운 것 → 어려운 것)**

1. `check_basic_language`, `check_labels`, `check_tables`, `check_keyboard`
2. `check_markup_errors`, `check_headings`, `check_response_time`, `check_flashing`
3. `check_video_captions`, `check_video_autoplay`, `check_contrast`
4. `check_pause_control` (스크롤 + async JS, 약 400줄)
5. `check_alt_text` (이미지 캡처 + OpenAI + 병렬 처리, 가장 복잡)

### 8.9 리포트 생성

| Python | Java |
|---|---|
| ReportLab `SimpleDocTemplate`, `Paragraph`, `Table`, `TableStyle` | OpenPDF `Document`, `Paragraph`, `PdfPTable`, `PdfPCell` |
| `pdfmetrics.registerFont(TTFont(...))` (한글 폰트) | `BaseFont.createFont("fonts/NanumGothic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)` |
| openpyxl `Workbook`, `Font`, `PatternFill`, `Alignment` | POI `XSSFWorkbook`, `Font`, `CellStyle.setFillForegroundColor`, `setAlignment` |
| `json.dumps` | Jackson `ObjectMapper.writeValueAsBytes` |

> [!NOTE]
> 한글 PDF를 위해 나눔고딕 등 TTF 폰트를 `src/main/resources/fonts/`에 넣고 임베드하세요. 다운로드 API는 `ResponseEntity<Resource>` + `Content-Disposition: attachment; filename*=UTF-8''...`로 반환합니다.

### 8.10 Controller 예시 (DRF 함수형 뷰 → `@RestController`)

```java
@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
@Tag(name = "Scan", description = "접근성 검사 API")
public class ScanController {
    private final ScanService scanService;

    @GetMapping("/")
    public PageResponse<ScanListItem> list(@AuthenticationPrincipal AuthUser me,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
                                           HttpServletRequest req) {
        return scanService.list(me.id(), page, Math.min(pageSize, 100), req);
    }

    @PostMapping("/create/")
    @ResponseStatus(HttpStatus.CREATED)
    public ScanCreateResponse create(@AuthenticationPrincipal AuthUser me,
                                     @Valid @RequestBody ScanCreateRequest body) {
        return scanService.create(me.id(), body.url());
    }

    @GetMapping("/{scanId}/")
    public ScanDetailResponse detail(@AuthenticationPrincipal AuthUser me, @PathVariable UUID scanId) {
        return scanService.detail(me.id(), scanId);
    }

    @DeleteMapping("/{scanId}/delete/")
    public Map<String, String> delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID scanId) {
        scanService.delete(me.id(), scanId);
        return Map.of("message", "검사가 삭제되었습니다.");
    }
    // progress, generate-report, download-report, restart, stream, alt-text-results ...
}
```

**DRF 페이지네이션 형식 유지**

```java
public record PageResponse<T>(long count, String next, String previous, List<T> results) {}
```

### 8.11 로깅 (LoggingContextMiddleware 대체)

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("logType", "Api");
        res.setHeader("X-Request-ID", requestId);
        try { chain.doFilter(req, res); } finally { MDC.clear(); }
    }
}
```

> `userId`는 JWT 필터에서 인증 성공 후 `MDC.put("userId", ...)`로 추가합니다. 비동기 스레드로 MDC를 넘기려면 `TaskDecorator`(`MdcTaskDecorator`)를 Executor에 등록합니다.

`logback-spring.xml` (RotatingFileHandler 10MB × 5개와 동일):

```xml
<configuration>
  <property name="PATTERN" value="[%d{yyyy-MM-dd HH:mm:ss}] [%level] [%X{logType:-System}] [Req:%X{requestId:--}] [Task:%X{taskId:--}] %msg%n"/>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder><pattern>${PATTERN}</pattern><charset>UTF-8</charset></encoder>
  </appender>
  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/webridge.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
      <fileNamePattern>logs/webridge.%i.log</fileNamePattern>
      <minIndex>1</minIndex><maxIndex>5</maxIndex>
    </rollingPolicy>
    <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
      <maxFileSize>10MB</maxFileSize>
    </triggeringPolicy>
    <encoder><pattern>${PATTERN}</pattern><charset>UTF-8</charset></encoder>
  </appender>
  <root level="INFO"><appender-ref ref="CONSOLE"/><appender-ref ref="FILE"/></root>
  <logger name="com.webridge" level="DEBUG"/>
</configuration>
```

### 8.12 Management Command 대체

| Django 명령 | Java 대체 |
|---|---|
| `create_initial_terms` | Flyway 데이터 마이그레이션 `V2__initial_terms.sql` 또는 `CommandLineRunner`(데이터 없을 때만 insert) |
| `cleanup_stuck_scans` | `ScanCleanupScheduler` (`@Scheduled`) + 관리자용 API `POST /api/v1/admin/scans/cleanup` |
| `setup_periodic_cleanup` | 불필요 (`@Scheduled`로 대체, 주기는 설정값으로) |
| `admin_cleanup_tools` | 관리자 전용 API (`hasRole('ADMIN')`) |
| `createsuperuser` | `CommandLineRunner` + 환경변수(`ADMIN_EMAIL`, `ADMIN_PASSWORD`) 또는 SQL |

---

## 9. 반드시 지켜야 할 호환성 포인트

프론트엔드 수정 없이 백엔드만 교체하려면 아래 항목을 지켜야 합니다.

### 9.1 URL 끝 슬래시 (가장 흔한 실수)

Django URL은 대부분 `/auth/login/`처럼 **끝에 `/`가 붙어 있습니다.** Spring Boot 3(Spring 6)는 **트레일링 슬래시 자동 매칭이 꺼져 있습니다.**

- 방법 A: 매핑을 기존 경로와 똑같이 작성: `@PostMapping("/login/")`
- 방법 B: Spring 6.2의 `UrlHandlerFilter`로 끝 슬래시를 제거하고 매핑은 슬래시 없이 작성

```java
@Bean
FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
    var filter = UrlHandlerFilter.trailingSlashHandler("/api/**").wrapRequest().build();
    var bean = new FilterRegistrationBean<>(filter);
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
}
```

`/user/me`는 원래 **슬래시가 없는** 경로이니 주의하세요.

### 9.2 JSON 형식

- 필드명: **snake_case** (`spring.jackson.property-naming-strategy: SNAKE_CASE`)
- 날짜: ISO-8601 문자열 (`2025-01-01T12:00:00+09:00`)
- UUID: 문자열
- 페이지네이션: `{ "count", "next", "previous", "results" }`
- 400 검증 오류: DRF 형식 `{ "field_name": ["오류 메시지"] }`
- 401/403/404: `{ "detail": "..." }`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> validation(MethodArgumentNotValidException e) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
            errors.computeIfAbsent(toSnake(fe.getField()), k -> new ArrayList<>()).add(fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getBody());   // 기존 {"error": ...} / {"detail": ...} 그대로
    }
}
```

> [!TIP]
> 뷰마다 에러 키가 `error`, `detail`, `message` 등으로 섞여 있습니다. Python 코드의 `Response({...}, status=...)`를 하나씩 보면서 **키 이름과 HTTP 상태 코드를 동일하게** 맞추세요.

### 9.3 비밀번호 해시 (기존 사용자 로그인 유지)

Django는 `pbkdf2_sha256$<반복횟수>$<salt>$<base64(hash)>` 형식으로 저장합니다. Spring 기본 인코더(BCrypt 등)는 이 형식을 읽지 못하므로 커스텀 인코더가 필요합니다.

```java
public class DjangoPbkdf2PasswordEncoder implements PasswordEncoder {
    private static final int ITERATIONS = 1_000_000;   // Django 5.2 기본값
    private final SecureRandom random = new SecureRandom();

    @Override
    public String encode(CharSequence raw) {
        String salt = randomSalt(22);
        return "pbkdf2_sha256$" + ITERATIONS + "$" + salt + "$" + hash(raw, salt, ITERATIONS);
    }

    @Override
    public boolean matches(CharSequence raw, String encoded) {
        if (encoded == null || encoded.startsWith("!")) return false;   // set_unusable_password()
        String[] p = encoded.split("\\$");
        if (p.length != 4 || !"pbkdf2_sha256".equals(p[0])) return false;
        String computed = hash(raw, p[2], Integer.parseInt(p[1]));
        return MessageDigest.isEqual(computed.getBytes(StandardCharsets.US_ASCII),
                                     p[3].getBytes(StandardCharsets.US_ASCII));
    }

    private String hash(CharSequence raw, String salt, int iterations) {
        try {
            var spec = new PBEKeySpec(raw.toString().toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8), iterations, 256);
            byte[] dk = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(dk);
        } catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }

    private String randomSalt(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        var sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }
}
```

> [!CAUTION]
> 소셜 로그인 사용자는 `set_unusable_password()`로 `!`로 시작하는 값이 저장되어 있습니다. 이 값을 비밀번호 로그인에 허용하면 안 됩니다(위 코드의 `startsWith("!")` 처리). 운영 DB의 실제 해시 하나로 **반드시 단위 테스트**를 작성하세요.

### 9.4 Django 소셜/이메일 인증 HTML 응답

`/auth/verify-email-link/`는 JSON이 아닌 **HTML 페이지**(`auth/verification_result.html`)를 반환합니다. 이 컨트롤러는 `@Controller` + Thymeleaf 뷰 이름 반환으로 구현합니다.

### 9.5 시간대

Django `USE_TZ=True`라 DB에는 UTC로 저장됩니다. JPA는 `OffsetDateTime` + `hibernate.jdbc.time_zone: UTC`를 사용하고, 응답만 `Asia/Seoul`로 직렬화합니다.

### 9.6 JSON 컬럼

`Survey.company_type` 등은 `["a","c"]` 형태의 JSON 배열입니다. `@JdbcTypeCode(SqlTypes.JSON) List<String>`으로 매핑하고, `clean()`의 선택값 검증은 커스텀 Validator 또는 Service에서 구현합니다.

### 9.7 WebSocket 메시지 형식

`{"type": "scan_progress", "data": {...}}` 형식과 경로 `/ws/scans/{uuid}/`를 그대로 유지합니다.

### 9.8 Django Admin

`core/admin.py`가 100KB 분량이라 Java로 바로 옮기기엔 부담이 큽니다.

- **전환기 권장**: Java API 서버와 별개로 **Django를 관리자 전용으로 계속 띄워 같은 DB를 바라보게** 합니다(`/admin/`만 노출). Java 쪽 비밀번호 해시를 Django 형식으로 유지(9.3)하면 관리자 로그인도 그대로 됩니다.
- **장기**: 관리자 전용 REST API(`/api/v1/admin/**`, `ROLE_ADMIN`)를 만들고 React-Admin 등으로 화면 구성.

> [!WARNING]
> Django를 관리자용으로 남기는 동안에는 **Django의 `migrate`를 실행하지 마세요.** 스키마 변경 권한은 Flyway(Java) 한 곳에만 두어야 충돌이 없습니다.

---

## 10. 실행 방법

### 10.1 최초 1회 준비

```powershell
# 1) 프로젝트 폴더로 이동
cd C:\dev\webridge-backend-java

# 2) 인프라 실행 (PostgreSQL, Redis)
docker compose up -d

# 3) .env 파일 작성 (7.1 참고)
notepad .env

# 4) Playwright Chromium 설치 (최초 1회, 약 150MB 다운로드)
.\gradlew.bat playwrightInstall
```

### 10.2 DB 스키마 준비 (둘 중 하나 선택)

**A. 새 DB로 시작 (개발용)**

Python 프로젝트에서 현재 스키마를 SQL로 뽑아 Flyway 첫 번째 마이그레이션으로 사용합니다.

```powershell
# Python 프로젝트에서 (가상환경 활성화 후) 로컬 Postgres에 Django 마이그레이션 적용
cd C:\dev\webridge-backend-main
$env:DATABASE_URL="postgres://webridge:webridge@localhost:5432/webridge"
python manage.py migrate

# 스키마만 덤프
docker exec webridge-postgres pg_dump -U webridge -d webridge --schema-only --no-owner > C:\dev\webridge-backend-java\src\main\resources\db\migration\V1__init.sql
```

> 이후 Django 전용 테이블(`django_*`, `auth_group*`, `core_user_groups` 등)을 남길지 결정하세요. Django Admin을 병행 운영할 계획이면 **남겨두어야** 합니다.

**B. 기존 운영 DB에 연결 (전환 시)**

- `V1__init.sql`은 기존 스키마와 동일하므로 실행되지 않도록 `baseline-on-migrate: true`, `baseline-version: 1` 설정(7.2에 포함)
- 이후 변경은 `V2__...sql`, `V3__...sql`로만 추가
- 반드시 **운영 DB 백업 후** 스테이징에서 먼저 검증

### 10.3 개발 서버 실행

```powershell
cd C:\dev\webridge-backend-java

# 실행 (local 프로필)
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

정상 실행되면:

| 항목 | 주소 |
|---|---|
| API | http://localhost:8080/api/v1/ping/ |
| Swagger | http://localhost:8080/api/docs |
| OpenAPI JSON | http://localhost:8080/api/schema |
| Health (Actuator) | http://localhost:8080/actuator/health |
| WebSocket | ws://localhost:8080/ws/scans/{uuid}/ |

> [!NOTE]
> 기존 Django는 8000 포트, Java는 8080 포트를 기본으로 사용합니다. 프론트엔드의 API Base URL 환경 변수만 바꾸면 두 서버를 번갈아 테스트할 수 있습니다. 같은 포트를 원하면 `server.port: 8000`으로 변경하세요.

### 10.4 기존 Python 실행 방식과의 비교

| 작업 | Python (기존) | Java (신규) |
|---|---|---|
| 의존성 설치 | `pip install -r requirements.txt` | `.\gradlew.bat build` (자동 다운로드) |
| 브라우저 설치 | `playwright install chromium` | `.\gradlew.bat playwrightInstall` |
| DB 마이그레이션 | `python manage.py migrate` | 앱 시작 시 Flyway 자동 실행 |
| 마이그레이션 생성 | `python manage.py makemigrations` | `src/main/resources/db/migration/V{n}__설명.sql` 직접 작성 |
| 서버 실행 | `python manage.py runserver` / `uvicorn webridge_drf.asgi:application` | `.\gradlew.bat bootRun` |
| 워커 실행 | `celery -A webridge_drf worker -Q accessibility,reports,default --pool=solo` | **불필요** (1단계에서는 앱 내부 Executor) |
| 스케줄러 | `celery -A webridge_drf beat` | **불필요** (`@Scheduled`) |
| 초기 약관 | `python manage.py create_initial_terms` | Flyway `V2__initial_terms.sql` |
| 테스트 | `python manage.py test` | `.\gradlew.bat test` |
| 배포용 빌드 | - | `.\gradlew.bat bootJar` → `build/libs/*.jar` |
| 운영 실행 | `gunicorn` / `uvicorn` + `celery worker` + `celery beat` (3개 프로세스) | `java -jar webridge-backend.jar` (1개 프로세스) |

### 10.5 JAR로 실행

```powershell
.\gradlew.bat clean bootJar
java -jar build\libs\webridge-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 11. 단계별 전환 로드맵

```mermaid
flowchart TD
    P0["0. 준비: 프로젝트 생성, 인프라, 공통 설정"] --> P1["1. DB/Entity: Flyway V1, 전체 Entity + Repository"]
    P1 --> P2["2. 인증: JWT, Django 비밀번호 호환, 회원가입/로그인/로그아웃/갱신"]
    P2 --> P3["3. 단순 CRUD: Health, User, Terms, Feedback, Survey, AltTextFeedback"]
    P3 --> P4["4. 이메일 인증, 비밀번호 재설정, 소셜 로그인"]
    P4 --> P5["5. 검사 엔진: AccessibilityChecker 13개 항목 이식"]
    P5 --> P6["6. 비동기 실행 + 진행률(WebSocket/SSE) + 정리 스케줄러"]
    P6 --> P7["7. 리포트: PDF/Excel/JSON"]
    P7 --> P8["8. 병행 운영 및 응답 비교 (스테이징)"]
    P8 --> P9["9. 운영 전환 + Django는 Admin 전용으로 축소"]
```

| 단계 | 작업 | 산출물 / 완료 기준 |
|---|---|---|
| 0 | Initializr 생성, docker-compose, `application.yml`, Jackson/CORS/Security 뼈대, 로깅 | `/api/v1/ping/` 응답 |
| 1 | `pg_dump`로 `V1__init.sql`, 23개 Entity, Repository | `ddl-auto: validate` 통과 |
| 2 | `JwtProvider`, `JwtAuthenticationFilter`, `DjangoPbkdf2PasswordEncoder`, 블랙리스트 | Django에서 가입한 계정으로 Java 로그인 성공, Django 발급 토큰으로 Java API 호출 성공 |
| 3 | User/Terms/Feedback/Survey/AltTextFeedback API | `docs/*-api.md`와 응답 일치 |
| 4 | 메일(Thymeleaf), 이메일 인증, 비밀번호 재설정, 소셜 로그인 3종 | 실제 메일 수신, 소셜 로그인 성공 |
| 5 | `checker-js/*.js` 분리, `AccessibilityChecker` 항목별 이식 | 같은 URL로 Python/Java 결과 비교 (8.8 순서) |
| 6 | `ScanTaskRunner`, `ScanProgressPublisher`, WebSocket/SSE, `ScanCleanupScheduler` | 프론트에서 검사 생성 → 진행률 표시 → 결과 확인 |
| 7 | PDF/Excel/JSON 리포트 | 한글 깨짐 없음, 다운로드 정상 |
| 8 | 스테이징 병행 운영 | 주요 시나리오 회귀 테스트 통과 |
| 9 | 운영 배포, 프론트 Base URL 전환, Celery 종료 | 모니터링 이상 없음 |

---

## 12. 테스트 및 검증

### 12.1 테스트 종류

| 종류 | 도구 | 대상 |
|---|---|---|
| 단위 테스트 | JUnit 5 + AssertJ | `DjangoPbkdf2PasswordEncoder`, `ContrastCalculator`, `compliance_score` 계산, `HeadingResult.total_issues` |
| Repository 테스트 | `@DataJpaTest` + Testcontainers(PostgreSQL) | Entity 매핑, JSON 컬럼 |
| API 테스트 | `@SpringBootTest` + `MockMvc` | 상태 코드, JSON 필드명(snake_case), 에러 형식 |
| 검사 엔진 테스트 | 로컬 HTML fixture(`src/test/resources/pages/*.html`) + Playwright | 항목별 검출 결과 |
| 비교(골든) 테스트 | 같은 URL을 Python/Java로 검사 후 결과 JSON diff | 이식 정확도 |

### 12.2 Python ↔ Java 응답 비교 방법

1. 두 서버를 동시에 실행 (Django: 8000, Java: 8080), **같은 DB**를 바라보게 설정
2. 같은 JWT 토큰으로 같은 요청을 보내 응답 비교

```powershell
$token = "eyJ..."   # 로그인 후 받은 access 토큰
curl.exe -s -H "Authorization: Bearer $token" http://localhost:8000/api/v1/scans/ > py.json
curl.exe -s -H "Authorization: Bearer $token" http://localhost:8080/api/v1/scans/ > java.json
code --diff py.json java.json
```

3. 접근성 검사는 같은 URL로 각각 실행 후 항목별 결과 개수/준수 여부 비교 (AI 결과는 매번 달라질 수 있으므로 개수와 `compliance` 분포 위주로 비교)

---

## 13. 배포

### 13.1 Dockerfile (Playwright 포함)

Playwright가 Chromium 의존 라이브러리를 필요로 하므로 **Microsoft 공식 Playwright Java 이미지**를 런타임 베이스로 쓰는 것이 가장 간단합니다.

```dockerfile
# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- runtime ----------
# Playwright Java 버전과 이미지 태그 버전을 맞출 것
FROM mcr.microsoft.com/playwright/java:v1.53.0-noble
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Seoul"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
```

> [!NOTE]
> 공식 이미지에 브라우저가 이미 설치되어 있으므로, 런타임에 다운로드하지 않도록 `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` 환경 변수를 설정하세요.

### 13.2 운영 설정 (`application-prod.yml`)

```yaml
server:
  forward-headers-strategy: framework   # 프록시(HTTPS) 뒤에서 실행
springdoc:
  swagger-ui:
    enabled: false                      # 필요 시만 활성화
logging:
  level:
    root: INFO
    com.webridge: INFO
management:
  endpoints.web.exposure.include: health,info
```

- 기존 Django의 `SECURE_HSTS_*`, `X_FRAME_OPTIONS=DENY` 등은 Spring Security `headers()` 설정으로 옮깁니다.
- HTTPS 리다이렉트는 보통 로드밸런서/프록시(AWS ALB, Railway 등)에서 처리합니다.

### 13.3 리소스 고려

- Chromium 1개당 메모리 300~500MB를 사용하므로 `scan.executor-pool-size`는 **컨테이너 메모리 / 500MB** 이하로 설정하세요.
- 기존 `CELERY_WORKER_MAX_MEMORY_PER_CHILD=512000` 역할은 "검사마다 Playwright를 새로 만들고 반드시 `close()`"하는 것으로 대신합니다(`try-with-resources`).

---

## 14. 체크리스트

### 기반
- [x] Spring Boot 프로젝트 생성 (Java 21, Gradle Kotlin DSL)
- [x] docker-compose (PostgreSQL, Redis) 실행
- [x] `.env` / `application.yml` 구성
- [x] Jackson snake_case, 시간대, 트레일링 슬래시 처리
- [x] 전역 예외 처리 (DRF 에러 형식)
- [x] MDC 로깅 + logback 파일 롤링
- [x] springdoc Swagger 태그 (Health/Auth/User/Scan/Feedback/Terms/Survey)

### 데이터
- [x] Flyway `V1__init.sql` (기존 스키마 덤프)
- [x] Entity 23개 + enum Converter (`cancelled` 포함)
- [x] `ddl-auto: validate` 통과
- [x] 초기 약관 데이터 (`create_initial_terms` 대체)

### 인증
- [x] Django PBKDF2 비밀번호 호환 + 단위 테스트
- [x] JWT (simplejwt claim 호환, 같은 SECRET_KEY)
- [x] Refresh Rotation + 블랙리스트
- [x] 비밀번호 재설정 토큰 (30분)
- [x] 이메일 인증 (링크, HTML 결과 페이지)
- [x] 소셜 로그인 (Google / Kakao / GitHub) + 약관 동의 플로우

### 기능 API
- [x] Health / Ping
- [x] User (`/user/me`, `/user/extra-info/`)
- [x] Terms (current / status / update)
- [x] Feedback CRUD + 페이지네이션(20)
- [x] Survey + SurveyTrigger
- [x] AltText Feedback
- [x] Scan CRUD + 페이지네이션(10)

### 검사 엔진
- [x] JS 스크립트 분리 (`checker-js/`)
- [x] 13개 검사 항목 이식 + fixture 테스트
- [x] OpenAI alt 텍스트 생성/평가 (프롬프트 동일)
- [x] `total_issues`, `compliance_score` 계산 로직 동일
- [x] 비동기 실행 (`@Async`, 커밋 후 실행), 타임아웃, 재시도
- [x] WebSocket `/ws/scans/{id}/` + SSE `/scans/{id}/stream/`
- [x] 중단된 검사 정리 스케줄러 (ShedLock)

### 리포트
- [x] PDF (OpenPDF, 한글 폰트)
- [x] Excel (Apache POI)
- [x] JSON
- [x] 다운로드 (파일명 UTF-8 인코딩, `download_count` 증가)

### 전환
- [x] Python/Java 응답 비교 테스트 (`scripts/compare_api.py`, `scripts/compare-api.ps1`)
- [x] Dockerfile (Playwright 이미지) + `.dockerignore`
- [ ] 스테이징 병행 운영
- [ ] 운영 DB 백업 → 전환 → 모니터링
- [ ] Django는 Admin 전용으로 축소 (또는 관리자 API 구축 후 제거)
- [ ] Celery worker / beat 종료
