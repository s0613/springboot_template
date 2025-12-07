# Spring Boot 서비스 템플릿 설계

## 개요

cogmo_back 프로젝트 구조를 기반으로 재사용 가능한 Spring Boot 서비스 템플릿을 생성한다.
새 프로젝트 시작 시 `init-project.sh` 스크립트로 프로젝트명/패키지명을 자동 치환하여 사용한다.

## 요구사항

| 항목 | 선택 |
|------|------|
| 인증 | OAuth2 전체 (Google, Kakao, Apple, Naver) + JWT |
| 결제 | 기본 구조 (엔티티, 서비스, 컨트롤러) |
| 치환 방식 | 초기화 스크립트 (`./init-project.sh`) |
| 알림 | Email + SMS + Push 전체 |
| 배포 | Docker Compose만 (클라우드는 프로젝트별) |
| 추가 기능 | 보안, 장애대응, AWS, 모니터링, WebSocket 전체 |

## 프로젝트 구조

```
infra_template/
├── src/main/java/com/template/app/
│   │
│   ├── auth/                           # 인증 모듈
│   │   ├── api/
│   │   │   ├── controller/             # AuthController, AccountController
│   │   │   └── dto/
│   │   │       ├── request/            # LoginRequest, SignupRequest 등
│   │   │       └── response/           # TokenResponse, UserResponse 등
│   │   ├── application/                # AuthService, OAuth2Service, SmsVerificationService
│   │   ├── domain/
│   │   │   ├── entity/                 # User
│   │   │   └── model/                  # UserPrincipal 등
│   │   └── infrastructure/
│   │       ├── config/                 # SecurityConfig
│   │       ├── security/               # JwtTokenProvider, JwtAuthenticationFilter
│   │       │                           # LoginAttemptService, OAuth2RateLimitFilter
│   │       ├── oauth2/
│   │       │   ├── google/             # GoogleTokenVerifierService
│   │       │   ├── kakao/              # KakaoTokenVerifierService
│   │       │   ├── apple/              # AppleTokenVerifierService
│   │       │   └── naver/              # NaverTokenVerifierService
│   │       ├── repository/             # UserRepository
│   │       └── exception/              # AuthenticationException 등
│   │
│   ├── common/                         # 공통 모듈
│   │   ├── config/
│   │   │   ├── aws/                    # AwsS3Config, AwsSesConfig, AwsSnsConfig
│   │   │   ├── cache/                  # RedisConfig, CacheConfig
│   │   │   ├── database/               # DatabaseConfig
│   │   │   ├── monitoring/             # HealthController, MetricsController
│   │   │   ├── security/               # RateLimitConfig
│   │   │   └── web/                    # WebSocketConfig, WebConfig
│   │   ├── exception/                  # GlobalExceptionHandler, ErrorResponse
│   │   ├── filter/                     # RateLimitFilter
│   │   ├── interceptor/                # RateLimitInterceptor
│   │   ├── integration/
│   │   │   ├── email/                  # EmailService, CircuitBreakerEmailService
│   │   │   └── sms/                    # SmsService, NcpSensClient
│   │   ├── service/                    # S3Service
│   │   ├── util/                       # 유틸리티 클래스
│   │   ├── dto/                        # 공통 DTO
│   │   └── ResilienceConfig.java       # Circuit Breaker 설정
│   │
│   ├── notification/                   # 알림 모듈
│   │   ├── controller/                 # NotificationController
│   │   ├── service/                    # PushNotificationService, EmailTemplateService
│   │   ├── entity/                     # EmailLog, PushToken 등
│   │   ├── repository/                 # EmailLogRepository
│   │   └── dto/
│   │
│   ├── payment/                        # 결제 모듈 (기본 구조)
│   │   ├── api/
│   │   │   ├── controller/             # PaymentController
│   │   │   └── dto/
│   │   │       ├── request/            # CreatePaymentRequest
│   │   │       └── response/           # PaymentResponse
│   │   ├── application/                # PaymentService
│   │   ├── domain/
│   │   │   └── entity/                 # Payment, PaymentStatus
│   │   └── infrastructure/
│   │       └── repository/             # PaymentRepository
│   │
│   └── TemplateApplication.java
│
├── src/main/resources/
│   ├── application.yml                 # 공통 설정
│   ├── application-local.yml           # 로컬 환경
│   ├── application-test.yml            # 테스트 환경
│   └── db/migration/
│       ├── V1__create_users_table.sql
│       ├── V2__create_payments_table.sql
│       └── V3__create_notifications_table.sql
│
├── src/test/java/com/template/app/
│   ├── auth/                           # 인증 테스트
│   ├── common/                         # 공통 테스트
│   └── support/                        # 테스트 지원 클래스
│
├── docker-compose.yml                  # Postgres + Redis
├── docker-compose.override.yml         # 개발 환경 오버라이드
├── Dockerfile
├── build.gradle
├── settings.gradle
├── init-project.sh                     # 초기화 스크립트
└── README.md
```

## 모듈별 상세

### 1. auth 모듈

cogmo_back의 auth 모듈을 그대로 복사하되, 패키지명만 변경한다.

**포함 기능:**
- JWT 토큰 발급/검증 (JwtTokenProvider)
- OAuth2 소셜 로그인 (Google, Kakao, Apple, Naver)
- SMS 인증 (SmsVerificationService)
- 로그인 시도 제한 (LoginAttemptService) - Redis 기반, 5회 실패 시 30분 차단
- OAuth2 Rate Limiting (OAuth2RateLimitFilter)
- Refresh Token 관리 (RefreshTokenService)

**복사 대상 파일:**
```
cogmo_back/src/main/java/com/cogmo/annyeong/auth/ → infra_template/src/main/java/com/template/app/auth/
```

### 2. common 모듈

**포함 기능:**

| 서브모듈 | 기능 |
|----------|------|
| config/aws | S3, SES, SNS 설정 |
| config/cache | Redis, Cache 설정 |
| config/database | DataSource, JPA 설정 |
| config/monitoring | Health, Metrics 엔드포인트 |
| config/security | Rate Limit 설정 |
| config/web | WebSocket, CORS 설정 |
| exception | GlobalExceptionHandler |
| filter | RateLimitFilter |
| integration | Email, SMS 서비스 |
| ResilienceConfig | Circuit Breaker (Resilience4j) |

**복사 대상:**
```
cogmo_back/src/main/java/com/cogmo/annyeong/common/ → infra_template/src/main/java/com/template/app/common/
```

### 3. notification 모듈

**포함 기능:**
- Email 발송 (AWS SES)
- SMS 발송 (NCP SENS)
- Push 알림 (AWS SNS)
- 발송 로그 저장

**복사 대상:**
```
cogmo_back/src/main/java/com/cogmo/annyeong/notification/ → infra_template/src/main/java/com/template/app/notification/
```

### 4. payment 모듈 (새로 생성)

기본 구조만 생성하고, 실제 PG 연동은 프로젝트별로 구현한다.

**생성할 파일:**
- `Payment.java` - 결제 엔티티
- `PaymentStatus.java` - 결제 상태 enum
- `PaymentService.java` - 결제 서비스 인터페이스
- `PaymentController.java` - 결제 API
- `PaymentRepository.java` - 결제 저장소

## 설정 파일

### application.yml (공통)

```yaml
spring:
  application:
    name: ${APP_NAME:template-app}

  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: ${JWT_SECRET}
  access-token-validity: 3600000      # 1 hour
  refresh-token-validity: 604800000   # 7 days

app:
  oauth2:
    allowed-origins: ${APP_OAUTH2_ALLOWED_ORIGINS:http://localhost:3000}

# AWS
aws:
  region: ${AWS_REGION:ap-northeast-2}
  s3:
    bucket: ${AWS_S3_BUCKET}
  ses:
    from-email: ${AWS_SES_FROM_EMAIL}

# NCP SENS (SMS)
ncp:
  sens:
    service-id: ${NCP_SENS_SERVICE_ID}
    access-key: ${NCP_SENS_ACCESS_KEY}
    secret-key: ${NCP_SENS_SECRET_KEY}
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: ${PROJECT_NAME}-postgres
    environment:
      POSTGRES_DB: ${PROJECT_NAME}_local
      POSTGRES_USER: ${PROJECT_NAME}
      POSTGRES_PASSWORD: ${PROJECT_NAME}123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${PROJECT_NAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: ${PROJECT_NAME}-redis
    command: redis-server --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres-data:
  redis-data:
```

## 초기화 스크립트

### init-project.sh

```bash
#!/bin/bash

# Usage: ./init-project.sh <project-name> <group-id>
# Example: ./init-project.sh my-awesome-app com.mycompany

PROJECT_NAME=$1
GROUP_ID=$2

if [ -z "$PROJECT_NAME" ] || [ -z "$GROUP_ID" ]; then
    echo "Usage: ./init-project.sh <project-name> <group-id>"
    echo "Example: ./init-project.sh my-awesome-app com.mycompany"
    exit 1
fi

# Convert project name to various formats
PROJECT_NAME_LOWER=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr '-' '_')
PROJECT_NAME_CAMEL=$(echo "$PROJECT_NAME" | sed -r 's/(^|-)(\w)/\U\2/g')
GROUP_PATH=$(echo "$GROUP_ID" | tr '.' '/')

echo "Initializing project: $PROJECT_NAME"
echo "Group ID: $GROUP_ID"
echo "Package: $GROUP_ID.$PROJECT_NAME_LOWER"

# 1. Rename package directories
mv src/main/java/com/template/app "src/main/java/$GROUP_PATH/$PROJECT_NAME_LOWER"
mv src/test/java/com/template/app "src/test/java/$GROUP_PATH/$PROJECT_NAME_LOWER"

# 2. Replace placeholders in all files
find . -type f \( -name "*.java" -o -name "*.yml" -o -name "*.xml" -o -name "*.gradle" -o -name "*.md" -o -name "Dockerfile" -o -name "docker-compose*.yml" \) -exec sed -i '' \
    -e "s/com\.template\.app/$GROUP_ID.$PROJECT_NAME_LOWER/g" \
    -e "s/template-app/$PROJECT_NAME/g" \
    -e "s/TemplateApplication/${PROJECT_NAME_CAMEL}Application/g" \
    -e "s/template_app/$PROJECT_NAME_LOWER/g" \
    {} \;

# 3. Rename main application file
mv "src/main/java/$GROUP_PATH/$PROJECT_NAME_LOWER/TemplateApplication.java" \
   "src/main/java/$GROUP_PATH/$PROJECT_NAME_LOWER/${PROJECT_NAME_CAMEL}Application.java"

# 4. Update settings.gradle
echo "rootProject.name = '$PROJECT_NAME'" > settings.gradle

# 5. Update build.gradle group
sed -i '' "s/group = 'com.template'/group = '$GROUP_ID'/g" build.gradle

echo "✅ Project initialized successfully!"
echo ""
echo "Next steps:"
echo "  1. Review and update application.yml with your configuration"
echo "  2. Set up environment variables or .env file"
echo "  3. Run: docker-compose up -d"
echo "  4. Run: ./gradlew bootRun"
```

## 구현 순서

1. **기본 구조 생성** - 디렉토리 구조, build.gradle, settings.gradle
2. **auth 모듈 복사** - cogmo_back에서 복사 후 패키지명 변경
3. **common 모듈 복사** - cogmo_back에서 복사 후 패키지명 변경
4. **notification 모듈 복사** - cogmo_back에서 복사 후 패키지명 변경
5. **payment 모듈 생성** - 기본 구조 새로 작성
6. **설정 파일 작성** - application*.yml, docker-compose.yml
7. **Flyway 마이그레이션** - 기본 테이블 생성 SQL
8. **init-project.sh 작성** - 초기화 스크립트
9. **README.md 작성** - 사용 가이드
10. **테스트 코드 정리** - 필요한 테스트만 포함

## 제외 항목

- ECS Task Definition (프로젝트별 구성)
- 배포 스크립트 (프로젝트별 구성)
- 도메인별 비즈니스 로직 (feedback, guardian, question, workbook 등)
- 프로젝트 특화 설정 (ScoringConfig 등)
