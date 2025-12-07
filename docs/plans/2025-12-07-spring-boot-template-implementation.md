# Spring Boot 서비스 템플릿 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** cogmo_back 기반의 재사용 가능한 Spring Boot 서비스 템플릿 생성

**Architecture:** Monolith 구조로 auth, common, notification, payment 모듈 포함. 플레이스홀더 패턴 사용하여 init-project.sh로 프로젝트명 자동 치환

**Tech Stack:** Java 21, Spring Boot 3.1, PostgreSQL, Redis, JWT, OAuth2, Resilience4j, Flyway

**Source:** `/Users/songseungju/cogmo/cogmo_back`
**Target:** `/Users/songseungju/infra_template`

**Platform:** macOS (sed -i '' 문법 사용)

---

## Task 1: 기본 프로젝트 구조 정리

**Files:**
- Delete: `src/main/java/com/example/infra_template/`
- Delete: `src/test/java/com/example/infra_template/`
- Create: `src/main/java/com/template/app/TemplateApplication.java`
- Modify: `build.gradle`
- Modify: `settings.gradle`

**Step 1: 기존 example 패키지 삭제**

```bash
rm -rf /Users/songseungju/infra_template/src/main/java/com/example
rm -rf /Users/songseungju/infra_template/src/test/java/com/example
```

**Step 2: 새 패키지 구조 생성**

```bash
mkdir -p /Users/songseungju/infra_template/src/main/java/com/template/app
mkdir -p /Users/songseungju/infra_template/src/test/java/com/template/app
```

**Step 3: TemplateApplication.java 생성**

```java
package com.template.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TemplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }
}
```

**Step 4: build.gradle 업데이트**

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.1.11'
    id 'io.spring.dependency-management' version '1.1.4'
    id 'jacoco'
}

group = 'com.template'
version = '0.0.1-SNAPSHOT'
description = 'Spring Boot Service Template'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

ext {
    set('testcontainersVersion', '1.19.3')
    set('resilience4jVersion', '2.1.0')
    set('bucket4jVersion', '8.7.0')
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'

    // WebSocket
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.webjars:sockjs-client:1.5.1'
    implementation 'org.webjars:stomp-websocket:2.3.4'

    // Rate Limiting
    implementation "com.bucket4j:bucket4j-core:${bucket4jVersion}"
    implementation "com.bucket4j:bucket4j-redis:${bucket4jVersion}"
    implementation 'javax.cache:cache-api:1.1.1'
    implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'

    // Circuit Breaker
    implementation "io.github.resilience4j:resilience4j-spring-boot3:${resilience4jVersion}"

    // Database
    implementation 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'

    // Hibernate JSONB Support
    implementation 'io.hypersistence:hypersistence-utils-hibernate-63:3.7.0'

    // Redis
    implementation 'redis.clients:jedis'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
    runtimeOnly 'org.bouncycastle:bcpkix-jdk15on:1.70'

    // Google OAuth2 ID Token Verification
    implementation 'com.google.api-client:google-api-client:2.2.0'

    // Utilities
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'

    // API Documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'

    // AWS S3
    implementation 'com.amazonaws:aws-java-sdk-s3:1.12.565'

    // AWS SES
    implementation 'com.amazonaws:aws-java-sdk-ses:1.12.565'

    // AWS SDK v2 for SNS
    implementation 'software.amazon.awssdk:sns:2.20.0'

    // NCP SENS SMS
    implementation 'org.apache.httpcomponents.client5:httpclient5:5.2.1'
    implementation 'commons-codec:commons-codec:1.16.0'

    // PDF Generation with Flying Saucer
    implementation 'org.xhtmlrenderer:flying-saucer-pdf:9.1.22'
    implementation 'org.xhtmlrenderer:flying-saucer-core:9.1.22'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:testcontainers'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    // Test Data Builder
    testImplementation 'com.github.javafaker:javafaker:1.0.2'

    // MockWebServer for OAuth2 testing
    testImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
}

dependencyManagement {
    imports {
        mavenBom "org.testcontainers:testcontainers-bom:${testcontainersVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
    ignoreFailures = true
}

jacoco {
    toolVersion = "0.8.11"
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}
```

**Step 5: settings.gradle 업데이트**

```gradle
rootProject.name = 'template-app'
```

**Step 6: 커밋**

```bash
git add -A && git commit -m "refactor: restructure to template package structure"
```

---

## Task 2: auth 모듈 복사

**Files:**
- Copy: `cogmo_back/src/main/java/com/cogmo/annyeong/auth/` → `infra_template/src/main/java/com/template/app/auth/`

**파일 목록 (41개):**
- `api/controller/`: AuthController.java, AccountController.java
- `api/dto/request/`: LoginRequest.java, SignupRequest.java, VerifyCodeRequest.java 등 7개
- `api/dto/response/`: TokenResponse.java, UserResponse.java 등 5개
- `domain/entity/`: User.java
- `application/`: AuthService.java, NativeOAuth2Service.java, RefreshTokenService.java 등 6개
- `infrastructure/security/`: JwtTokenProvider.java, JwtAuthenticationFilter.java 등 4개
- `infrastructure/oauth2/`: AppleTokenVerifierService.java, GoogleTokenVerifierService.java 등 3개
- `infrastructure/config/`: SecurityConfig.java
- `infrastructure/repository/`: UserRepository.java
- `infrastructure/exception/`: 9개 예외 클래스

**Step 1: auth 디렉토리 복사**

```bash
cp -r /Users/songseungju/cogmo/cogmo_back/src/main/java/com/cogmo/annyeong/auth \
      /Users/songseungju/infra_template/src/main/java/com/template/app/
```

**Step 2: 패키지명 치환 (macOS)**

```bash
find /Users/songseungju/infra_template/src/main/java/com/template/app/auth \
  -name "*.java" -exec sed -i '' 's/com\.cogmo\.annyeong/com.template.app/g' {} \;
```

**Step 3: 도메인 특화 import 확인**

> auth 모듈은 외부 도메인 의존성이 없음 (독립적)

**Step 4: 컴파일 확인**

```bash
cd /Users/songseungju/infra_template && ./gradlew compileJava
```

**Step 5: 커밋**

```bash
git add -A && git commit -m "feat: add auth module from cogmo_back"
```

---

## Task 3: common 모듈 복사

**Files:**
- Copy: `cogmo_back/src/main/java/com/cogmo/annyeong/common/` → `infra_template/src/main/java/com/template/app/common/`

**파일 목록 (34개):**
- `config/aws/`: AwsS3Config.java, AwsS3Properties.java, AwsSesConfig.java 등 5개
- `config/cache/`: CacheConfig.java, RedisConfig.java 등
- `config/monitoring/`: HealthController.java, MetricsController.java 등 3개
- `dto/`: ApiResponse.java, FileUploadResponse.java
- `exception/`: GlobalExceptionHandler.java, ErrorResponse.java 등
- `controller/`: FileController.java
- `filter/`: RateLimitFilter.java
- `integration/email/`: CircuitBreakerEmailService.java
- `integration/sms/`: SmsService.java, NcpSensClient.java
- `service/`: S3Service.java

**Step 1: common 디렉토리 복사**

```bash
cp -r /Users/songseungju/cogmo/cogmo_back/src/main/java/com/cogmo/annyeong/common \
      /Users/songseungju/infra_template/src/main/java/com/template/app/
```

**Step 2: 패키지명 치환 (macOS)**

```bash
find /Users/songseungju/infra_template/src/main/java/com/template/app/common \
  -name "*.java" -exec sed -i '' 's/com\.cogmo\.annyeong/com.template.app/g' {} \;
```

**Step 3: GlobalExceptionHandler 도메인 특화 참조 제거**

파일: `common/exception/GlobalExceptionHandler.java`

**제거할 import 및 핸들러 (11개):**

```java
// 제거할 import
import com.template.app.workbook.exception.*;  // 8개 클래스
import com.template.app.feedback.exception.*;  // 2개 클래스
import com.template.app.question.exception.*; // 3개 클래스

// 제거할 @ExceptionHandler 메서드들:
// - handleSessionNotFound
// - handleSessionNotInProgress
// - handleSessionNotCompleted
// - handleDuplicateAnswer
// - handleNoAvailableWorkbook
// - handleWorkbookNotFound
// - handleInvalidWorkbookState
// - handleWorkbookQuestionLimitExceeded
// - handleEmptyWorkbookPublish
// - handleInvalidQuestionReorder
// - handleInvalidTempIdReference
// - handleReportGeneration
// - handleFeedbackCache
// - handleQuestionNotFound
// - handleInvalidQuestionType
// - handleUnauthorizedQuestionAccess
```

**Step 4: 컴파일 확인**

```bash
cd /Users/songseungju/infra_template && ./gradlew compileJava
```

**Step 5: 커밋**

```bash
git add -A && git commit -m "feat: add common module from cogmo_back"
```

---

## Task 4: notification 모듈 복사

**Files:**
- Copy: `cogmo_back/src/main/java/com/cogmo/annyeong/notification/` → `infra_template/src/main/java/com/template/app/notification/`

**파일 목록 (13개):**
- `domain/`: EmailRequest.java, EmailTemplate.java
- `entity/`: EmailLog.java, FailedNotification.java, PushToken.java
- `repository/`: EmailLogRepository.java, FailedNotificationRepository.java, PushTokenRepository.java
- `service/`: EmailService.java, PushNotificationService.java, EmailTemplateService.java, DeadLetterQueueService.java
- `exception/`: EmailSendException.java

**Step 1: notification 디렉토리 복사**

```bash
cp -r /Users/songseungju/cogmo/cogmo_back/src/main/java/com/cogmo/annyeong/notification \
      /Users/songseungju/infra_template/src/main/java/com/template/app/
```

**Step 2: 패키지명 치환 (macOS)**

```bash
find /Users/songseungju/infra_template/src/main/java/com/template/app/notification \
  -name "*.java" -exec sed -i '' 's/com\.cogmo\.annyeong/com.template.app/g' {} \;
```

**Step 3: EmailService WorkbookSession 의존성 제거**

파일: `notification/service/EmailService.java`

**변경 전:**
```java
import com.template.app.workbook.entity.WorkbookSession;

public void sendAssessmentCompleteEmail(User user, WorkbookSession session, String pdfUrl) {
    // session.getScore(), session.getRiskLevel() 등 사용
}
```

**변경 후:**
```java
// WorkbookSession import 제거

// DTO로 대체
public record AssessmentResult(int score, String riskLevel, LocalDateTime completedAt) {}

public void sendAssessmentCompleteEmail(User user, AssessmentResult result, String pdfUrl) {
    // result.score(), result.riskLevel() 등 사용
}
```

**Step 4: 컴파일 확인**

```bash
cd /Users/songseungju/infra_template && ./gradlew compileJava
```

**Step 5: 커밋**

```bash
git add -A && git commit -m "feat: add notification module from cogmo_back"
```

---

## Task 5: payment 모듈 생성 (기본 구조)

**Files:**
- Create: `src/main/java/com/template/app/payment/domain/entity/Payment.java`
- Create: `src/main/java/com/template/app/payment/domain/entity/PaymentStatus.java`
- Create: `src/main/java/com/template/app/payment/infrastructure/repository/PaymentRepository.java`
- Create: `src/main/java/com/template/app/payment/application/PaymentService.java`
- Create: `src/main/java/com/template/app/payment/api/controller/PaymentController.java`
- Create: `src/main/java/com/template/app/payment/api/dto/request/CreatePaymentRequest.java`
- Create: `src/main/java/com/template/app/payment/api/dto/response/PaymentResponse.java`

**Step 1: 디렉토리 구조 생성**

```bash
mkdir -p /Users/songseungju/infra_template/src/main/java/com/template/app/payment/{domain/entity,infrastructure/repository,application,api/{controller,dto/{request,response}}}
```

**Step 2: PaymentStatus.java 생성**

```java
package com.template.app.payment.domain.entity;

public enum PaymentStatus {
    PENDING,      // 결제 대기
    COMPLETED,    // 결제 완료
    FAILED,       // 결제 실패
    CANCELLED,    // 결제 취소
    REFUNDED      // 환불 완료
}
```

**Step 3: Payment.java 생성**

```java
package com.template.app.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column
    private String paymentKey;  // PG사 결제 키

    @Column
    private String paymentMethod;

    @Column
    private String failReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void complete(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.COMPLETED;
    }

    public void fail(String reason) {
        this.failReason = reason;
        this.status = PaymentStatus.FAILED;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
    }
}
```

**Step 4: PaymentRepository.java 생성**

```java
package com.template.app.payment.infrastructure.repository;

import com.template.app.payment.domain.entity.Payment;
import com.template.app.payment.domain.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByPaymentKey(String paymentKey);
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);
}
```

**Step 5: CreatePaymentRequest.java 생성**

```java
package com.template.app.payment.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CreatePaymentRequest {

    @NotBlank(message = "주문 ID는 필수입니다")
    private String orderId;

    @NotNull(message = "결제 금액은 필수입니다")
    @DecimalMin(value = "100", message = "최소 결제 금액은 100원입니다")
    private BigDecimal amount;

    @NotBlank(message = "통화는 필수입니다")
    private String currency;

    @NotBlank(message = "결제 수단은 필수입니다")
    private String paymentMethod;
}
```

**Step 6: PaymentResponse.java 생성**

```java
package com.template.app.payment.api.dto.response;

import com.template.app.payment.domain.entity.Payment;
import com.template.app.payment.domain.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {
    private Long id;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentKey;
    private String paymentMethod;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentKey(payment.getPaymentKey())
                .paymentMethod(payment.getPaymentMethod())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
```

**Step 7: PaymentService.java 생성**

```java
package com.template.app.payment.application;

import com.template.app.payment.api.dto.request.CreatePaymentRequest;
import com.template.app.payment.api.dto.response.PaymentResponse;
import com.template.app.payment.domain.entity.Payment;
import com.template.app.payment.domain.entity.PaymentStatus;
import com.template.app.payment.infrastructure.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse createPayment(Long userId, CreatePaymentRequest request) {
        Payment payment = Payment.builder()
                .userId(userId)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: orderId={}, userId={}", request.getOrderId(), userId);

        return PaymentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + orderId));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPayments(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse completePayment(String orderId, String paymentKey) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + orderId));

        payment.complete(paymentKey);
        log.info("Payment completed: orderId={}, paymentKey={}", orderId, paymentKey);

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + orderId));

        payment.cancel();
        log.info("Payment cancelled: orderId={}", orderId);

        return PaymentResponse.from(payment);
    }
}
```

**Step 8: PaymentController.java 생성**

> 주의: `@AuthenticationPrincipal Long userId`는 auth 모듈의 UserPrincipal과 연동 필요.
> SecurityConfig에서 UserPrincipal.getId()를 Long으로 resolve하는 ArgumentResolver 추가 권장.

```java
package com.template.app.payment.api.controller;

import com.template.app.payment.api.dto.request.CreatePaymentRequest;
import com.template.app.payment.api.dto.response.PaymentResponse;
import com.template.app.payment.application.PaymentService;
import com.template.app.auth.domain.model.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.createPayment(userPrincipal.getId(), request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPayment(orderId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.getUserPayments(userPrincipal.getId()));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<PaymentResponse> completePayment(
            @PathVariable String orderId,
            @RequestParam String paymentKey) {
        return ResponseEntity.ok(paymentService.completePayment(orderId, paymentKey));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.cancelPayment(orderId));
    }
}
```

**Step 9: 컴파일 확인**

```bash
cd /Users/songseungju/infra_template && ./gradlew compileJava
```

**Step 10: 커밋**

```bash
git add -A && git commit -m "feat: add payment module with basic structure"
```

---

## Task 6: 설정 파일 작성

**Files:**
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-local.yml`
- Create: `src/main/resources/application-test.yml`
- Delete: `src/main/resources/application.properties`

**Step 1: application.properties 삭제**

```bash
rm -f /Users/songseungju/infra_template/src/main/resources/application.properties
```

**Step 2: application.yml 생성**

```yaml
spring:
  application:
    name: ${APP_NAME:template-app}

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/template_local}
    username: ${DATABASE_USERNAME:template}
    password: ${DATABASE_PASSWORD:template123}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:10}
      minimum-idle: ${DB_MIN_IDLE:5}
      connection-timeout: ${DB_CONNECTION_TIMEOUT:30000}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_batch_fetch_size: 10
        jdbc:
          batch_size: 20
        order_inserts: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    out-of-order: true

  data:
    web:
      pageable:
        max-page-size: 100
        default-page-size: 20
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: ${REDIS_TIMEOUT:2000}
      jedis:
        pool:
          max-active: ${REDIS_MAX_ACTIVE:8}
          max-idle: ${REDIS_MAX_IDLE:8}
          min-idle: ${REDIS_MIN_IDLE:0}

  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: Asia/Seoul

  task:
    execution:
      pool:
        core-size: 2
        max-size: 5
        queue-capacity: 100
      thread-name-prefix: async-task-

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
            scope:
              - email
              - profile
            redirect-uri: "{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}"
          kakao:
            client-id: ${KAKAO_CLIENT_ID:}
            client-secret: ${KAKAO_CLIENT_SECRET:}
            scope:
              - profile_nickname
              - account_email
            client-name: Kakao
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}"
            client-authentication-method: client_secret_post
          apple:
            client-id: ${APPLE_CLIENT_ID:}
            client-secret: ${APPLE_CLIENT_SECRET:}
            authorization-grant-type: authorization_code
            scope:
              - name
              - email
            client-name: Apple
            redirect-uri: "{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}"
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
          apple:
            authorization-uri: https://appleid.apple.com/auth/authorize
            token-uri: https://appleid.apple.com/auth/token
            jwk-set-uri: https://appleid.apple.com/auth/keys

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:changeme-this-is-a-very-long-secret-key-for-jwt-token-generation-minimum-256-bits}
  access-token-validity: 3600000      # 1 hour
  refresh-token-validity: 1209600000  # 14 days

# OAuth2 Application Configuration
app:
  oauth2:
    redirect-uri:
      web: ${APP_OAUTH2_REDIRECT_URI_WEB:http://localhost:3000/auth/callback}
      mobile: ${APP_OAUTH2_REDIRECT_URI_MOBILE:template://auth/callback}
    allowed-origins: ${APP_OAUTH2_ALLOWED_ORIGINS:http://localhost:3000}
    google:
      client-id-android: ${GOOGLE_CLIENT_ID_ANDROID:}
      client-id-ios: ${GOOGLE_CLIENT_ID_IOS:}
      client-id-server: ${GOOGLE_CLIENT_ID_SERVER:}
      client-id-web: ${GOOGLE_CLIENT_ID_WEB:}
    kakao:
      native-app-key: ${KAKAO_NATIVE_APP_KEY:}
  base-url: ${APP_BASE_URL:http://localhost:8080}
  frontend-url: ${APP_FRONTEND_URL:http://localhost:3000}
  rate-limit:
    enabled: ${APP_RATE_LIMIT_ENABLED:true}
    oauth2:
      requests-per-minute: 10

server:
  port: 8080
  forward-headers-strategy: framework
  error:
    include-message: always
    include-binding-errors: always
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true

logging:
  level:
    com.template: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# API Configuration
api:
  version: v1
  rate-limit:
    default-requests-per-minute: 60
    burst-capacity: 100

# Resilience4j Circuit Breaker
resilience4j:
  circuitbreaker:
    instances:
      sms-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
      email-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
      sns-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s

# AWS Configuration
aws:
  s3:
    bucket-name: ${AWS_S3_BUCKET:}
    region: ${AWS_REGION:ap-northeast-2}
    access-key: ${AWS_ACCESS_KEY:}
    secret-key: ${AWS_SECRET_KEY:}
    presigned-url-expiration: 60
  ses:
    region: ${AWS_REGION:ap-northeast-2}
    access-key: ${AWS_ACCESS_KEY:}
    secret-key: ${AWS_SECRET_KEY:}
    from-email: ${AWS_SES_FROM_EMAIL:noreply@example.com}
    from-name: ${AWS_SES_FROM_NAME:Template App}
    enabled: ${AWS_SES_ENABLED:false}
    use-iam-credentials: ${AWS_USE_IAM_CREDENTIALS:false}
  sns:
    region: ${AWS_REGION:ap-northeast-2}
    access-key: ${AWS_ACCESS_KEY:}
    secret-key: ${AWS_SECRET_KEY:}
    topic-arn: ${AWS_SNS_TOPIC_ARN:}
    enabled: ${AWS_SNS_ENABLED:false}
    use-iam-credentials: ${AWS_USE_IAM_CREDENTIALS:false}
    platform-application-arn:
      android: ${AWS_SNS_ANDROID_ARN:}
      ios: ${AWS_SNS_IOS_ARN:}

# NCP SENS Configuration
ncp:
  sens:
    service-id: ${NCP_SENS_SERVICE_ID:}
    access-key: ${NCP_SENS_ACCESS_KEY:}
    secret-key: ${NCP_SENS_SECRET_KEY:}
    from-phone: ${NCP_SENS_FROM_PHONE:}
    enabled: ${NCP_SENS_ENABLED:false}
    api-url: https://sens.apigw.ntruss.com
    sms:
      type: SMS
      content-type: COMM
    retry:
      max-attempts: 3
      backoff-ms: 1000
```

**Step 3: application-local.yml 생성**

```yaml
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true

  datasource:
    url: jdbc:postgresql://localhost:5432/template_local
    username: template
    password: template123

  data:
    redis:
      host: localhost
      port: 6379
      password: ""

logging:
  level:
    com.template: DEBUG
    org.springframework.security: DEBUG
```

**Step 4: application-test.yml 생성**

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:15-alpine:///test_db
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  jpa:
    hibernate:
      ddl-auto: create-drop

  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: test-secret-key-for-jwt-token-generation-minimum-256-bits-required
  access-token-validity: 3600000
  refresh-token-validity: 604800000

aws:
  ses:
    enabled: false
  sns:
    enabled: false

ncp:
  sens:
    enabled: false
```

**Step 5: 커밋**

```bash
git add -A && git commit -m "feat: add application configuration files"
```

---

## Task 7: Docker 설정 파일 작성

**Files:**
- Modify: `Dockerfile`
- Modify: `docker-compose.yml`
- Create: `docker-compose.override.yml`

**Step 1: Dockerfile 작성**

```dockerfile
# Production stage - uses pre-built JAR
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy pre-built JAR from local build
COPY build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 2: docker-compose.yml 작성**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: template-postgres
    environment:
      POSTGRES_DB: template_local
      POSTGRES_USER: template
      POSTGRES_PASSWORD: template123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U template"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: template-redis
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

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: template-app
    environment:
      SPRING_PROFILES_ACTIVE: local
      DATABASE_URL: jdbc:postgresql://postgres:5432/template_local
      DATABASE_USERNAME: template
      DATABASE_PASSWORD: template123
      REDIS_HOST: redis
      REDIS_PORT: 6379
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  postgres-data:
  redis-data:
```

**Step 3: docker-compose.override.yml 작성**

```yaml
version: '3.8'

# Development overrides
services:
  postgres:
    ports:
      - "5433:5432"  # 다른 포트로 매핑 (충돌 방지)

  app:
    build:
      context: .
      dockerfile: Dockerfile
    volumes:
      - ./build/libs:/app/libs:ro
    environment:
      SPRING_DEVTOOLS_RESTART_ENABLED: "true"
```

**Step 4: 커밋**

```bash
git add -A && git commit -m "feat: add Docker configuration"
```

---

## Task 8: Flyway 마이그레이션 작성

**Files:**
- Create: `src/main/resources/db/migration/V1__create_users_table.sql`
- Create: `src/main/resources/db/migration/V2__create_payments_table.sql`
- Create: `src/main/resources/db/migration/V3__create_notifications_table.sql`

**Step 1: 마이그레이션 디렉토리 생성**

```bash
mkdir -p /Users/songseungju/infra_template/src/main/resources/db/migration
```

**Step 2: V1__create_users_table.sql 생성**

```sql
-- Create users table with OAuth support
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) UNIQUE,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    birth_date DATE,
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),

    -- OAuth fields
    oauth_provider VARCHAR(50),
    oauth_id VARCHAR(255),

    -- Account type
    user_type VARCHAR(20) NOT NULL DEFAULT 'MASTER' CHECK (user_type IN ('MASTER', 'SUB_ACCOUNT')),
    account_type VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (account_type IN ('USER', 'ADMIN')),
    master_user_id BIGINT,

    -- Consent fields
    consent_privacy BOOLEAN NOT NULL DEFAULT FALSE,
    consent_service BOOLEAN NOT NULL DEFAULT FALSE,
    consent_marketing BOOLEAN NOT NULL DEFAULT FALSE,

    -- Status fields
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,

    -- Self-reference for sub-accounts
    CONSTRAINT fk_users_master FOREIGN KEY (master_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_users_phone_number ON users(phone_number);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE UNIQUE INDEX idx_users_oauth ON users(oauth_provider, oauth_id) WHERE oauth_provider IS NOT NULL;

-- Comments
COMMENT ON TABLE users IS 'User accounts with OAuth and password authentication support';
COMMENT ON COLUMN users.oauth_provider IS 'OAuth provider: google, kakao, apple, naver';
COMMENT ON COLUMN users.user_type IS 'MASTER: main account, SUB_ACCOUNT: linked account';
```

**Step 3: V2__create_payments_table.sql 생성**

```sql
-- Create payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    payment_key VARCHAR(255),
    payment_method VARCHAR(50),
    fail_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- Comments
COMMENT ON TABLE payments IS 'Payment records for orders';
COMMENT ON COLUMN payments.payment_key IS 'PG provider payment key';
COMMENT ON COLUMN payments.status IS 'PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED';
```

**Step 4: V3__create_notifications_table.sql 생성**

```sql
-- Email logs table
CREATE TABLE email_logs (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENT', 'FAILED', 'PENDING')),
    error_message TEXT,
    message_id VARCHAR(255),
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_logs_recipient_email ON email_logs(recipient_email);
CREATE INDEX idx_email_logs_status ON email_logs(status);
CREATE INDEX idx_email_logs_sent_at ON email_logs(sent_at);
CREATE INDEX idx_email_logs_recipient_sent_at ON email_logs(recipient_email, sent_at);

-- Push tokens table
CREATE TABLE push_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(500) NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('ANDROID', 'IOS')),
    endpoint_arn VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_push_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_push_tokens_user_id ON push_tokens(user_id);
CREATE INDEX idx_push_tokens_enabled ON push_tokens(enabled);

-- Failed notifications (Dead Letter Queue)
CREATE TABLE failed_notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(20) NOT NULL CHECK (notification_type IN ('EMAIL', 'SMS', 'PUSH')),
    recipient_user_id BIGINT,
    recipient_address VARCHAR(500) NOT NULL,
    message_content TEXT NOT NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RETRYING', 'FAILED', 'SUCCEEDED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_retry_at TIMESTAMP,
    succeeded_at TIMESTAMP,

    CONSTRAINT fk_failed_notifications_user FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_failed_notifications_retry ON failed_notifications(next_retry_at, status)
    WHERE status IN ('PENDING', 'RETRYING') AND retry_count < max_retries;
CREATE INDEX idx_failed_notifications_status ON failed_notifications(status, created_at);

-- Comments
COMMENT ON TABLE email_logs IS 'Email send attempt logs';
COMMENT ON TABLE push_tokens IS 'Device push notification tokens (FCM/APNS)';
COMMENT ON TABLE failed_notifications IS 'Dead Letter Queue for failed notifications with retry logic';
```

**Step 5: 커밋**

```bash
git add -A && git commit -m "feat: add Flyway migrations"
```

---

## Task 9: init-project.sh 작성

**Files:**
- Create: `init-project.sh`

**Step 1: init-project.sh 생성**

```bash
#!/bin/bash

# Spring Boot Service Template - Project Initializer
# Usage: ./init-project.sh <project-name> <group-id>
# Example: ./init-project.sh my-awesome-app com.mycompany

set -e

PROJECT_NAME=$1
GROUP_ID=$2

if [ -z "$PROJECT_NAME" ] || [ -z "$GROUP_ID" ]; then
    echo "Usage: ./init-project.sh <project-name> <group-id>"
    echo "Example: ./init-project.sh my-awesome-app com.mycompany"
    exit 1
fi

# Detect OS for sed compatibility
if [[ "$OSTYPE" == "darwin"* ]]; then
    SED_INPLACE="sed -i ''"
else
    SED_INPLACE="sed -i"
fi

# Convert project name to various formats
PROJECT_NAME_LOWER=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr '-' '_')
PROJECT_NAME_CAMEL=$(echo "$PROJECT_NAME" | sed -E 's/(^|-)([a-z])/\U\2/g')
GROUP_PATH=$(echo "$GROUP_ID" | tr '.' '/')

echo "========================================="
echo "Initializing project: $PROJECT_NAME"
echo "Group ID: $GROUP_ID"
echo "Package: $GROUP_ID.$PROJECT_NAME_LOWER"
echo "========================================="

# 1. Create new package directory structure
echo "Step 1: Creating package directories..."
mkdir -p "src/main/java/$GROUP_PATH/$PROJECT_NAME_LOWER"
mkdir -p "src/test/java/$GROUP_PATH/$PROJECT_NAME_LOWER"

# 2. Move existing modules to new location
echo "Step 2: Moving modules to new package..."
for module in auth common notification payment; do
    if [ -d "src/main/java/com/template/app/$module" ]; then
        mv "src/main/java/com/template/app/$module" "src/main/java/$GROUP_PATH/$PROJECT_NAME_LOWER/"
    fi
done

# Move main application
if [ -f "src/main/java/com/template/app/TemplateApplication.java" ]; then
    mv "src/main/java/com/template/app/TemplateApplication.java" \
       "src/main/java/$GROUP_PATH/$PROJECT_NAME_LOWER/${PROJECT_NAME_CAMEL}Application.java"
fi

# Move test files
for module in auth common support; do
    if [ -d "src/test/java/com/template/app/$module" ]; then
        mv "src/test/java/com/template/app/$module" "src/test/java/$GROUP_PATH/$PROJECT_NAME_LOWER/"
    fi
done

# 3. Remove old template directories
echo "Step 3: Cleaning up old directories..."
rm -rf src/main/java/com/template
rm -rf src/test/java/com/template

# 4. Replace package names in all Java files
echo "Step 4: Replacing package names in Java files..."
find . -type f -name "*.java" -exec $SED_INPLACE \
    -e "s/com\.template\.app/$GROUP_ID.$PROJECT_NAME_LOWER/g" \
    -e "s/TemplateApplication/${PROJECT_NAME_CAMEL}Application/g" \
    {} \;

# 5. Update configuration files
echo "Step 5: Updating configuration files..."

# settings.gradle
echo "rootProject.name = '$PROJECT_NAME'" > settings.gradle

# build.gradle
$SED_INPLACE "s/group = 'com\.template'/group = '$GROUP_ID'/g" build.gradle
$SED_INPLACE "s/template-app/$PROJECT_NAME/g" build.gradle

# application.yml files
find src/main/resources -name "*.yml" -exec $SED_INPLACE \
    -e "s/template-app/$PROJECT_NAME/g" \
    -e "s/template_local/${PROJECT_NAME_LOWER}_local/g" \
    -e "s/template:/${PROJECT_NAME_LOWER}:/g" \
    -e "s/template123/${PROJECT_NAME_LOWER}123/g" \
    -e "s/com\.template/$GROUP_ID/g" \
    {} \;

# docker-compose files
find . -name "docker-compose*.yml" -exec $SED_INPLACE \
    -e "s/template-/$PROJECT_NAME-/g" \
    -e "s/template_local/${PROJECT_NAME_LOWER}_local/g" \
    -e "s/POSTGRES_USER: template/POSTGRES_USER: $PROJECT_NAME_LOWER/g" \
    -e "s/POSTGRES_PASSWORD: template123/POSTGRES_PASSWORD: ${PROJECT_NAME_LOWER}123/g" \
    -e "s/DATABASE_USERNAME: template/DATABASE_USERNAME: $PROJECT_NAME_LOWER/g" \
    -e "s/DATABASE_PASSWORD: template123/DATABASE_PASSWORD: ${PROJECT_NAME_LOWER}123/g" \
    -e "s/pg_isready -U template/pg_isready -U $PROJECT_NAME_LOWER/g" \
    {} \;

# 6. Update README.md
echo "Step 6: Updating README.md..."
$SED_INPLACE \
    -e "s/template-app/$PROJECT_NAME/g" \
    -e "s/com\.template/$GROUP_ID/g" \
    README.md

# 7. Clean up sed backup files (macOS creates these)
find . -name "*.bak" -o -name "*''" -delete 2>/dev/null || true

echo ""
echo "========================================="
echo "✅ Project initialized successfully!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. Review and update src/main/resources/application.yml"
echo "  2. Copy .env.example to .env and configure"
echo "  3. Start infrastructure: docker-compose up -d"
echo "  4. Run application: ./gradlew bootRun"
echo ""
echo "Package structure:"
echo "  src/main/java/$GROUP_PATH/$PROJECT_NAME_LOWER/"
echo ""
```

**Step 2: 실행 권한 부여**

```bash
chmod +x /Users/songseungju/infra_template/init-project.sh
```

**Step 3: 커밋**

```bash
git add -A && git commit -m "feat: add project initialization script"
```

---

## Task 10: 환경 설정 파일 작성

**Files:**
- Create: `.env.example`
- Modify: `.gitignore`

**Step 1: .env.example 생성**

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/template_local
DATABASE_USERNAME=template
DATABASE_PASSWORD=template123

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT (MUST change in production - minimum 256 bits)
JWT_SECRET=changeme-this-is-a-very-long-secret-key-for-jwt-token-generation-minimum-256-bits

# OAuth2 - Google
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_CLIENT_ID_ANDROID=
GOOGLE_CLIENT_ID_IOS=
GOOGLE_CLIENT_ID_WEB=

# OAuth2 - Kakao
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
KAKAO_NATIVE_APP_KEY=

# OAuth2 - Apple
APPLE_CLIENT_ID=
APPLE_CLIENT_SECRET=

# AWS
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_S3_BUCKET=
AWS_SES_FROM_EMAIL=noreply@example.com
AWS_SES_FROM_NAME=Template App
AWS_SES_ENABLED=false
AWS_SNS_ENABLED=false
AWS_SNS_TOPIC_ARN=
AWS_SNS_ANDROID_ARN=
AWS_SNS_IOS_ARN=

# NCP SENS (SMS)
NCP_SENS_SERVICE_ID=
NCP_SENS_ACCESS_KEY=
NCP_SENS_SECRET_KEY=
NCP_SENS_FROM_PHONE=
NCP_SENS_ENABLED=false

# Application
APP_BASE_URL=http://localhost:8080
APP_FRONTEND_URL=http://localhost:3000
APP_OAUTH2_ALLOWED_ORIGINS=http://localhost:3000
APP_RATE_LIMIT_ENABLED=true
SPRING_PROFILES_ACTIVE=local
```

**Step 2: .gitignore 업데이트**

```gitignore
HELP.md
.gradle
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache
bin/
!**/src/main/**/bin/
!**/src/test/**/bin/

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr
out/
!**/src/main/**/out/
!**/src/test/**/out/

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/

### VS Code ###
.vscode/

### Environment Variables ###
.env
.env.local
.env.*.local

### Secrets ###
**/secrets/
*.pem
*.key
*_accessKeys.csv

### Logs ###
logs/
*.log

### OS Files ###
.DS_Store
Thumbs.db
```

**Step 3: 커밋**

```bash
git add -A && git commit -m "feat: add environment configuration files"
```

---

## Task 11: README.md 작성

**Files:**
- Modify: `README.md`

**Step 1: README.md 작성**

```markdown
# Spring Boot Service Template

재사용 가능한 Spring Boot 서비스 템플릿입니다.

## 포함 기능

| 카테고리 | 기능 |
|---------|------|
| **인증** | OAuth2 (Google/Kakao/Apple) + JWT + SMS 인증 |
| **보안** | Rate Limiting, 로그인 시도 제한, CORS |
| **장애 대응** | Circuit Breaker (Resilience4j) |
| **알림** | Email (AWS SES), SMS (NCP SENS), Push (AWS SNS) |
| **결제** | 기본 구조 (PG 연동은 프로젝트별 구현) |
| **인프라** | Docker Compose, PostgreSQL 15, Redis 7 |
| **모니터링** | Actuator, Health Check, Metrics |

## 빠른 시작

### 1. 템플릿 복사

```bash
cp -r infra_template my-new-project
cd my-new-project
rm -rf .git && git init
```

### 2. 프로젝트 초기화

```bash
./init-project.sh my-awesome-app com.mycompany
```

이 스크립트는:
- 패키지명을 `com.mycompany.my_awesome_app`으로 변경
- 애플리케이션 클래스를 `MyAwesomeAppApplication`으로 이름 변경
- 설정 파일의 플레이스홀더를 프로젝트명으로 치환

### 3. 환경 설정

```bash
cp .env.example .env
# .env 파일을 편집하여 실제 값 입력
```

### 4. 인프라 실행

```bash
docker-compose up -d
```

### 5. 애플리케이션 실행

```bash
./gradlew bootRun
```

## 환경변수

### 필수

| 변수 | 설명 |
|------|------|
| `DATABASE_URL` | PostgreSQL 연결 URL |
| `DATABASE_USERNAME` | DB 사용자명 |
| `DATABASE_PASSWORD` | DB 비밀번호 |
| `REDIS_HOST` | Redis 호스트 |
| `JWT_SECRET` | JWT 서명 키 (최소 256비트) |

### OAuth2 (선택)

| 변수 | 설명 |
|------|------|
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID |
| `KAKAO_CLIENT_ID` | Kakao OAuth2 Client ID |
| `APPLE_CLIENT_ID` | Apple OAuth2 Client ID |

### AWS (선택)

| 변수 | 설명 |
|------|------|
| `AWS_S3_BUCKET` | S3 버킷명 |
| `AWS_SES_FROM_EMAIL` | SES 발신 이메일 |
| `AWS_SNS_TOPIC_ARN` | SNS 토픽 ARN |

## 프로젝트 구조

```
src/main/java/com/{groupId}/{projectName}/
├── auth/                    # 인증/인가
│   ├── api/controller/      # REST 컨트롤러
│   ├── api/dto/             # 요청/응답 DTO
│   ├── application/         # 서비스 레이어
│   ├── domain/entity/       # User 엔티티
│   └── infrastructure/      # Security, OAuth2, Repository
├── common/                  # 공통 모듈
│   ├── config/              # AWS, Cache, DB, Web 설정
│   ├── exception/           # 전역 예외 핸들러
│   └── integration/         # Email, SMS 연동
├── notification/            # 알림 모듈
│   ├── entity/              # EmailLog, PushToken
│   ├── service/             # Email, Push 서비스
│   └── repository/
├── payment/                 # 결제 모듈 (기본 구조)
│   ├── api/
│   ├── application/
│   ├── domain/entity/
│   └── infrastructure/
└── Application.java
```

## API 문서

애플리케이션 실행 후 Swagger UI 접속:
- http://localhost:8080/swagger-ui.html

## 테스트

```bash
# 전체 테스트
./gradlew test

# 테스트 리포트 생성
./gradlew jacocoTestReport
```

## Docker

### 개발 환경

```bash
# 인프라만 (PostgreSQL + Redis)
docker-compose up -d postgres redis

# 전체 (앱 포함)
docker-compose up -d
```

### 프로덕션 빌드

```bash
./gradlew clean build
docker build -t my-app:latest .
```

## 라이선스

MIT License
```

**Step 2: 커밋**

```bash
git add -A && git commit -m "docs: add comprehensive README"
```

---

## Task 12: 테스트 코드 정리

**Files:**
- Copy: `cogmo_back/src/test/java/com/cogmo/annyeong/auth/` → `infra_template/src/test/java/com/template/app/auth/`
- Copy: `cogmo_back/src/test/java/com/cogmo/annyeong/common/` → `infra_template/src/test/java/com/template/app/common/`
- Copy: `cogmo_back/src/test/java/com/cogmo/annyeong/support/` → `infra_template/src/test/java/com/template/app/support/`

**Step 1: 테스트 디렉토리 복사**

```bash
cp -r /Users/songseungju/cogmo/cogmo_back/src/test/java/com/cogmo/annyeong/auth \
      /Users/songseungju/infra_template/src/test/java/com/template/app/
cp -r /Users/songseungju/cogmo/cogmo_back/src/test/java/com/cogmo/annyeong/common \
      /Users/songseungju/infra_template/src/test/java/com/template/app/
cp -r /Users/songseungju/cogmo/cogmo_back/src/test/java/com/cogmo/annyeong/support \
      /Users/songseungju/infra_template/src/test/java/com/template/app/
```

**Step 2: 패키지명 치환 (macOS)**

```bash
find /Users/songseungju/infra_template/src/test/java/com/template/app \
  -name "*.java" -exec sed -i '' 's/com\.cogmo\.annyeong/com.template.app/g' {} \;
```

**Step 3: 도메인 특화 테스트 제거**

테스트 파일에서 feedback, workbook, guardian, question 관련 import/테스트 제거

**Step 4: 테스트 실행**

```bash
cd /Users/songseungju/infra_template && ./gradlew test
```

**Step 5: 커밋**

```bash
git add -A && git commit -m "test: add auth and common module tests"
```

---

## Task 13: 최종 검증 및 정리

**Step 1: 전체 빌드**

```bash
cd /Users/songseungju/infra_template && ./gradlew clean build
```

**Step 2: 불필요한 파일 정리**

```bash
# .gitkeep 파일 제거
find . -name ".gitkeep" -delete

# sed 백업 파일 제거 (macOS)
find . -name "*.bak" -o -name "*''" -delete 2>/dev/null || true
```

**Step 3: init-project.sh 테스트**

```bash
# 테스트용 디렉토리에서 실행
cd /tmp
rm -rf test-project
cp -r /Users/songseungju/infra_template test-project
cd test-project
./init-project.sh test-app com.testcompany
./gradlew clean build
```

**Step 4: 최종 커밋**

```bash
cd /Users/songseungju/infra_template
git add -A && git commit -m "chore: final cleanup and verification"
```

---

## 완료 체크리스트

- [ ] Task 1: 기본 프로젝트 구조 정리
- [ ] Task 2: auth 모듈 복사
- [ ] Task 3: common 모듈 복사 (GlobalExceptionHandler 정리 포함)
- [ ] Task 4: notification 모듈 복사 (EmailService 수정 포함)
- [ ] Task 5: payment 모듈 생성
- [ ] Task 6: 설정 파일 작성 (application.yml 전체 내용)
- [ ] Task 7: Docker 설정 파일 작성
- [ ] Task 8: Flyway 마이그레이션 작성 (전체 SQL)
- [ ] Task 9: init-project.sh 작성 (크로스 플랫폼 지원)
- [ ] Task 10: 환경 설정 파일 작성 (.env.example, .gitignore)
- [ ] Task 11: README.md 작성
- [ ] Task 12: 테스트 코드 정리
- [ ] Task 13: 최종 검증 및 정리
