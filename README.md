# WeBridge Backend (Java 21 / Spring Boot 3)

웹 접근성 진단 및 개선 제안 플랫폼 **WeBridge**의 백엔드 서비스입니다.  
기존 Python/Django + Celery 아키텍처에서 **Java 21 + Spring Boot 3.4.1**로 완전 마이그레이션되었습니다.

---

## 🚀 기술 스택

- **언어 및 런타임**: Java 21 (Eclipse Temurin 21 권장)
- **프레임워크**: Spring Boot 3.4.1
- **빌드 도구**: Gradle 8.11 (Kotlin DSL)
- **데이터베이스 및 ORM**:
  - PostgreSQL 16 (운영/개발)
  - Spring Data JPA (Hibernate 6)
  - Flyway (스키마 형상 관리)
  - H2 Database (인메모리 단위/통합 테스트)
- **캐시 및 메시징**: Redis 7, Spring Data Redis
- **스케줄링 & 분산 락**: Spring `@Scheduled` + ShedLock (Redis Provider)
- **웹 접근성 검사 엔진**: Playwright Java 1.53.0 (Chromium 기반) + OpenAI GPT-4o Client
- **보안 & 인증**:
  - Spring Security 6 (Stateless JWT)
  - JJWT 0.12.6 (Django simplejwt 토큰 완벽 호환)
  - Django PBKDF2 PasswordEncoder (기존 계정 및 Django Admin 호환)
  - Redis 기반 Access/Refresh Token 블랙리스트 & 토큰 회전(Rotation)
- **실시간 진행률**:
  - WebSocket (`/ws/scans/{scanId}/`)
  - Server-Sent Events (`/api/v1/scans/{scanId}/stream/`)
- **리포트 생성**:
  - PDF: OpenPDF 2.0.3 (나눔고딕 한글 폰트 내장)
  - Excel: Apache POI 5.4.1 (`poi-ooxml`)
  - JSON: Jackson (`ObjectMapper`)
- **문서화**: SpringDoc OpenAPI 2.8.9 (Swagger UI)

---

## 📁 디렉터리 구조

```
webridge-backend-java/
├── .github/                     # CI/CD 워크플로우
├── docs/                        # 마이그레이션 가이드 및 API 규격 문서
│   └── JAVA_MIGRATION_GUIDE.md
├── scripts/                     # API 응답 비교 및 검증 도구
│   ├── compare-api.ps1
│   └── compare_api.py
├── src/
│   ├── main/
│   │   ├── java/com/webridge/
│   │   │   ├── alttextfeedback/ # 대체 텍스트 피드백
│   │   │   ├── auth/            # JWT, PBKDF2, 소셜 로그인, 이메일 인증
│   │   │   ├── common/          # 공통 응답(snake_case), 전역 예외, MDC 로깅
│   │   │   ├── config/          # Security, Web, WebSocket, Async, ShedLock 설정
│   │   │   ├── feedback/        # 사용자 일반 피드백
│   │   │   ├── health/          # Health / Ping
│   │   │   ├── mail/            # Thymeleaf HTML 메일 발송 서비스
│   │   │   ├── scan/            # 접근성 검사 엔진, 비동기 러너, 리포트 생성, CRUD API
│   │   │   ├── survey/          # 설문조사 & 스캔 연동 트리거
│   │   │   ├── terms/           # 약관 관리
│   │   │   └── user/            # 사용자 엔티티 & 프로필
│   │   └── resources/
│   │       ├── checker-js/      # 13개 접근성 검사 JavaScript 스크립트
│   │       ├── db/migration/    # Flyway 마이그레이션 SQL (V1, V2)
│   │       ├── fonts/           # PDF용 나눔고딕 TTF 폰트
│   │       ├── templates/       # Thymeleaf 이메일 HTML 템플릿
│   │       ├── application.yml
│   │       └── application-prod.yml
│   └── test/                    # 65개 단위/통합 테스트 (JUnit 5 + MockMvc)
├── Dockerfile                   # Playwright 런타임 내장 멀티스테이지 Dockerfile
├── docker-compose.yml           # PostgreSQL & Redis 로컬 개발 환경
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🛠️ 시작 가이드

### 1. 사전 요구사항
- **JDK 21** 이상 설치
- **Docker** 및 **Docker Compose** 실행 중

### 2. 인프라 실행 (PostgreSQL & Redis)
```powershell
docker compose up -d
```

### 3. 환경 변수 설정
프로젝트 루트 디렉터리에 `.env` 파일을 준비합니다:
```properties
DATABASE_URL_JDBC=jdbc:postgresql://localhost:5432/webridge
DB_USERNAME=webridge
DB_PASSWORD=webridge
SECRET_KEY=your-32-byte-secret-key-for-jwt-hs256!
OPENAI_API_KEY=your-openai-api-key
REDIS_HOST=localhost
REDIS_PORT=6379
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_HOST_USER=
EMAIL_HOST_PASSWORD=
FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
```

### 4. Playwright 브라우저 설치 (최초 1회)
```powershell
.\gradlew.bat playwrightInstall
```

### 5. 개발 서버 실행
```powershell
.\gradlew.bat bootRun
```

실행 후 브라우저에서 아래 엔드포인트에 접속할 수 있습니다:
- **API Ping**: `http://localhost:8080/api/v1/ping/`
- **Swagger UI**: `http://localhost:8080/api/docs`
- **OpenAPI Schema**: `http://localhost:8080/api/schema`
- **Actuator Health**: `http://localhost:8080/actuator/health`
- **WebSocket 진행률**: `ws://localhost:8080/ws/scans/{scanId}/`

---

## 🧪 테스트 실행

전체 단위 및 통합 테스트(65개) 실행:
```powershell
.\gradlew.bat test
```

---

## 📦 운영 빌드 및 배포

### 1. 실행 가능 JAR 빌드
```powershell
.\gradlew.bat bootJar
```
빌드된 파일은 `build/libs/webridge-backend-0.0.1-SNAPSHOT.jar`에 생성됩니다.

JAR 직접 실행:
```powershell
java -jar build/libs/webridge-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 2. Docker 컨테이너 빌드 및 실행
Playwright 브라우저 의존성이 사전 설치된 공식 Playwright 런타임 이미지를 기반으로 빌드됩니다:
```powershell
docker build -t webridge-backend:latest .
docker run -p 8080:8080 --env-file .env webridge-backend:latest
```

---

## ⚖️ Python ↔ Java 응답 동등성 검증 (Parity Test)

스테이징 환경에서 기존 Django 서버(포트 8000)와 신규 Java 서버(포트 8080)의 응답 동등성을 비교 검증할 수 있습니다:

```powershell
# PowerShell 사용 시
.\scripts\compare-api.ps1 -DjangoUrl "http://localhost:8000" -JavaUrl "http://localhost:8080" -Token "<jwt_access_token>"

# Python CLI 사용 시
python scripts/compare_api.py --py http://localhost:8000 --java http://localhost:8080 --token "<jwt_access_token>"
```

---

## 📄 라이선스
This project is licensed under the MIT License.
