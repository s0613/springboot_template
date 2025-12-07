# Spring Boot Service Template

프로덕션 레벨의 Spring Boot 3.x 서비스 템플릿입니다. 인증, 알림, 결제 및 인프라 컴포넌트를 포함합니다.

## 이 템플릿을 만든 이유

새로운 백엔드 서비스를 시작할 때마다 반복되는 인프라 코드 작성을 줄이기 위해 만들었습니다.
- JWT 인증, OAuth2 연동
- AWS/NCP 클라우드 서비스 통합
- Rate limiting, Circuit breaker
- 결제 연동 구조

이런 공통 기능들을 미리 구현해두고, `init-project.sh` 스크립트로 패키지명만 바꿔서 바로 사용할 수 있습니다.

## 주요 기능

### 인증 (Auth)
- JWT 기반 인증 (Access Token + Refresh Token)
- OAuth2 소셜 로그인 (Google, Kakao, Apple)
- SMS 본인인증 (NCP SENS)
- 로그인 시도 제한 및 계정 잠금
- 마스터/서브 계정 구조

### 알림 (Notification)
- 이메일 발송 (AWS SES + Thymeleaf 템플릿)
- SMS 발송 (NCP SENS)
- 푸시 알림 (AWS SNS - Android/iOS)
- Dead Letter Queue (실패한 알림 재시도)

### 결제 (Payment)
- PG 연동 템플릿 (Toss Payments, Iamport 호환)
- 결제 생명주기 관리 (생성 → 승인 → 취소)
- 부분 환불 지원

### 인프라
- Rate Limiting (Bucket4j + Redis)
- Circuit Breaker (Resilience4j)
- Redis 캐싱 + Caffeine 로컬 캐시
- AWS S3 파일 업로드
- Flyway 데이터베이스 마이그레이션
- Docker + Docker Compose

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.1.11 |
| Database | PostgreSQL 15 |
| Cache | Redis 7 |
| Build | Gradle 8.14 |
| Container | Docker, Docker Compose |
| Cloud | AWS (S3, SES, SNS), NCP (SENS) |

## 빠른 시작

### 1. 템플릿 클론

```bash
git clone https://github.com/s0613/springboot_template.git my-project
cd my-project
```

### 2. 프로젝트 초기화

```bash
./init-project.sh my-awesome-app com.company.myapp
```

이 스크립트는:
- 패키지명을 `com.template.app` → `com.company.myapp`으로 변경
- 설정 파일들의 프로젝트명 업데이트
- 메인 클래스명 변경 (TemplateApplication → MyAwesomeAppApplication)

### 3. 환경 설정

```bash
cp .env.example .env
# .env 파일을 열어 필요한 값들을 설정
```

### 4. 개발 환경 실행

```bash
# PostgreSQL, Redis 시작
docker-compose -f docker-compose.dev.yml up -d

# 애플리케이션 실행
./gradlew bootRun
```

### 5. 접속 확인

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health

## 프로젝트 구조

```
src/main/java/com/template/app/
├── auth/                           # 인증 모듈
│   ├── api/
│   │   ├── controller/             # AuthController, AccountController
│   │   └── dto/                    # Request/Response DTOs
│   ├── domain/
│   │   └── entity/                 # User, RefreshToken
│   ├── infrastructure/
│   │   ├── oauth2/                 # Google, Kakao, Apple 연동
│   │   └── security/               # JWT, Security Config
│   └── service/                    # AuthService, SmsVerificationService
│
├── common/                         # 공통 모듈
│   ├── config/
│   │   ├── aws/                    # S3, SES, SNS 설정
│   │   ├── cache/                  # Redis, Caffeine 설정
│   │   └── monitoring/             # Health, Metrics
│   ├── dto/                        # ApiResponse
│   ├── exception/                  # GlobalExceptionHandler
│   └── integration/                # SmsService, 외부 연동
│
├── notification/                   # 알림 모듈
│   ├── domain/                     # EmailTemplate, EmailRequest
│   ├── entity/                     # EmailLog, PushToken, FailedNotification
│   ├── repository/                 # JPA Repositories
│   └── service/                    # EmailService, PushNotificationService, DLQ
│
└── payment/                        # 결제 모듈
    ├── api/
    │   ├── controller/             # PaymentController
    │   └── dto/                    # PaymentRequest, PaymentResponse
    ├── domain/
    │   └── entity/                 # Payment
    └── service/                    # PaymentService
```

## 환경 변수 설정

`.env.example` 파일 참조. 주요 설정:

```bash
# 필수 설정
DATABASE_URL=jdbc:postgresql://localhost:5432/myapp_db
DATABASE_USERNAME=myapp_user
DATABASE_PASSWORD=secure_password
JWT_SECRET=your-256-bit-secret-key-here  # 최소 32자

# AWS (선택)
AWS_S3_ENABLED=true
AWS_S3_BUCKET=my-bucket
AWS_ACCESS_KEY=AKIA...
AWS_SECRET_KEY=...

# NCP SMS (선택)
NCP_SMS_ENABLED=true
NCP_ACCESS_KEY=...
NCP_SECRET_KEY=...
NCP_SMS_SERVICE_ID=...
NCP_SMS_SENDER_PHONE=01012345678

# OAuth2 (선택)
GOOGLE_CLIENT_ID=...
KAKAO_CLIENT_ID=...
```

## API 엔드포인트

### 인증 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 (전화번호 + 비밀번호) |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| POST | `/api/v1/auth/logout` | 로그아웃 |
| POST | `/api/v1/auth/oauth2/{provider}` | OAuth2 로그인 |
| POST | `/api/v1/auth/sms/send` | SMS 인증코드 발송 |
| POST | `/api/v1/auth/sms/verify` | SMS 인증코드 확인 |

### 결제 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/payments/init` | 결제 시작 |
| POST | `/api/v1/payments/confirm` | 결제 승인 |
| GET | `/api/v1/payments/{id}` | 결제 조회 |
| GET | `/api/v1/payments/my` | 내 결제 내역 |
| POST | `/api/v1/payments/{id}/cancel` | 결제 취소 |

### 모니터링 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/actuator/health` | 헬스 체크 |
| GET | `/actuator/metrics` | 메트릭스 |
| GET | `/api/v1/infrastructure/status` | 인프라 상태 |
| GET | `/api/v1/infrastructure/dlq/stats` | DLQ 통계 |

## 데이터베이스 마이그레이션

Flyway 마이그레이션 파일 위치: `src/main/resources/db/migration/`

| 버전 | 파일명 | 내용 |
|------|--------|------|
| V1 | create_users_table.sql | 사용자 테이블 |
| V2 | create_auth_tables.sql | 인증 테이블 (토큰, 로그인시도, SMS) |
| V3 | create_notification_tables.sql | 알림 테이블 (이메일, 푸시, DLQ) |
| V4 | create_payments_table.sql | 결제 테이블 |

새 마이그레이션 추가:
```bash
touch src/main/resources/db/migration/V5__description.sql
```

## Docker 배포

### 개발 환경 (DB만)

```bash
docker-compose -f docker-compose.dev.yml up -d
```

### 전체 스택 배포

```bash
docker-compose up -d
```

### Docker 이미지 빌드

```bash
docker build -t my-app:latest .
```

## 커스터마이징 가이드

### OAuth2 프로바이더 추가

1. `auth/infrastructure/oauth2/`에 새 토큰 검증 서비스 추가
2. `NativeOAuth2Service`에 프로바이더 케이스 추가
3. 환경 변수 설정

### 이메일 템플릿 추가

1. `notification/domain/EmailTemplate` enum에 추가
2. `src/main/resources/templates/email/`에 템플릿 파일 생성
3. `EmailService`에 발송 메서드 추가

### PG 연동 구현

`PaymentService`의 TODO 주석 부분을 실제 PG API 호출로 교체:

```java
// confirmPayment() 메서드에서:
// TODO: Call PG provider's confirm API here
TossPaymentsResponse response = tossPaymentsClient.confirmPayment(request);
```

## 라이선스

MIT License

## 기여

이슈와 PR 환영합니다!
