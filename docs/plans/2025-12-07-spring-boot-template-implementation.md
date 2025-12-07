# Spring Boot 서비스 템플릿 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** cogmo_back 기반의 재사용 가능한 Spring Boot 서비스 템플릿 생성

**Architecture:** Monolith 구조로 auth, common, notification, payment 모듈 포함. 플레이스홀더 패턴 사용하여 init-project.sh로 프로젝트명 자동 치환

**Tech Stack:** Java 21, Spring Boot 3.1, PostgreSQL, Redis, JWT, OAuth2, Resilience4j, Flyway

**Source:** `/Users/songseungju/cogmo/cogmo_back`
**Target:** `/Users/songseungju/infra_template`

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

cogmo_back/build.gradle 내용을 복사하되:
- `group = 'com.template'`
- `description = 'Spring Boot Service Template'`
- 도메인 특화 의존성 제거 (feedback, workbook 관련)

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

**Step 1: auth 디렉토리 복사**

```bash
cp -r /Users/songseungju/cogmo/cogmo_back/src/main/java/com/cogmo/annyeong/auth \
      /Users/songseungju/infra_template/src/main/java/com/template/app/
```

**Step 2: 패키지명 치환**

모든 .java 파일에서:
- `com.cogmo.annyeong` → `com.template.app`

```bash
find /Users/songseungju/infra_template/src/main/java/com/template/app/auth \
  -name "*.java" -exec sed -i '' 's/com\.cogmo\.annyeong/com.template.app/g' {} \;
```

**Step 3: 도메인 특화 import 제거**

cogmo 특화 import 확인 후 제거 또는 주석 처리

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

**Step 1: common 디렉토리 복사**

```bash
cp -r /Users/songseungju/cogmo/cogmo_back/src/main/java/com/cogmo/annyeong/common \
      /Users/songseungju/infra_template/src/main/java/com/template/app/
```

**Step 2: 패키지명 치환**

```bash
find /Users/songseungju/infra_template/src/main/java/com/template/app/common \
  -name "*.java" -exec sed -i '' 's/com\.cogmo\.annyeong/com.template.app/g' {} \;
```

**Step 3: 도메인 특화 참조 제거**

feedback, workbook, guardian, question 관련 import/코드 제거

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

**Step 1: notification 디렉토리 복사**

```bash
cp -r /Users/songseungju/cogmo/cogmo_back/src/main/java/com/cogmo/annyeong/notification \
      /Users/songseungju/infra_template/src/main/java/com/template/app/
```

**Step 2: 패키지명 치환**

```bash
find /Users/songseungju/infra_template/src/main/java/com/template/app/notification \
  -name "*.java" -exec sed -i '' 's/com\.cogmo\.annyeong/com.template.app/g' {} \;
```

**Step 3: 도메인 특화 참조 제거**

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

```java
package com.template.app.payment.api.controller;

import com.template.app.payment.api.dto.request.CreatePaymentRequest;
import com.template.app.payment.api.dto.response.PaymentResponse;
import com.template.app.payment.application.PaymentService;
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
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.createPayment(userId, request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPayment(orderId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(paymentService.getUserPayments(userId));
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
rm /Users/songseungju/infra_template/src/main/resources/application.properties
```

**Step 2: application.yml 생성**

cogmo_back/src/main/resources/application.yml을 복사하되 프로젝트 특화 설정 제거

**Step 3: application-local.yml 복사**

```bash
cp /Users/songseungju/cogmo/cogmo_back/src/main/resources/application-local.yml \
   /Users/songseungju/infra_template/src/main/resources/
```

**Step 4: application-test.yml 복사**

```bash
cp /Users/songseungju/cogmo/cogmo_back/src/main/resources/application-test.yml \
   /Users/songseungju/infra_template/src/main/resources/
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

**Step 1: Dockerfile 복사 및 수정**

cogmo_back/Dockerfile 복사

**Step 2: docker-compose.yml 수정**

cogmo_back/docker-compose.yml 복사하되:
- container_name을 `template-*`로 변경
- database 이름을 `template_local`로 변경

**Step 3: docker-compose.override.yml 복사**

```bash
cp /Users/songseungju/cogmo/cogmo_back/docker-compose.override.yml \
   /Users/songseungju/infra_template/
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

User 엔티티 기반 테이블 생성 SQL

**Step 3: V2__create_payments_table.sql 생성**

```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    status VARCHAR(20) NOT NULL,
    payment_key VARCHAR(255),
    payment_method VARCHAR(50),
    fail_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
```

**Step 4: V3__create_notifications_table.sql 생성**

cogmo_back에서 notification 관련 마이그레이션 복사

**Step 5: 커밋**

```bash
git add -A && git commit -m "feat: add Flyway migrations"
```

---

## Task 9: init-project.sh 작성

**Files:**
- Create: `init-project.sh`

**Step 1: init-project.sh 생성**

설계 문서의 init-project.sh 내용 사용

**Step 2: 실행 권한 부여**

```bash
chmod +x /Users/songseungju/infra_template/init-project.sh
```

**Step 3: 커밋**

```bash
git add -A && git commit -m "feat: add project initialization script"
```

---

## Task 10: README.md 작성

**Files:**
- Modify: `README.md`

**Step 1: README.md 작성**

```markdown
# Spring Boot Service Template

재사용 가능한 Spring Boot 서비스 템플릿입니다.

## 포함 기능

- **인증**: OAuth2 (Google/Kakao/Apple/Naver) + JWT
- **보안**: Rate Limiting, 로그인 시도 제한, CORS
- **장애 대응**: Circuit Breaker (Resilience4j)
- **알림**: Email (SES), SMS (NCP SENS), Push (SNS)
- **결제**: 기본 구조 (PG 연동은 별도)
- **인프라**: Docker Compose, PostgreSQL, Redis

## 사용 방법

### 1. 템플릿 복사

\`\`\`bash
cp -r infra_template my-new-project
cd my-new-project
\`\`\`

### 2. 프로젝트 초기화

\`\`\`bash
./init-project.sh my-awesome-app com.mycompany
\`\`\`

### 3. 환경 설정

\`\`\`bash
# .env 파일 생성 또는 환경변수 설정
cp .env.example .env
# .env 파일 수정
\`\`\`

### 4. 실행

\`\`\`bash
# Docker로 DB 실행
docker-compose up -d

# 애플리케이션 실행
./gradlew bootRun
\`\`\`

## 환경변수

| 변수 | 설명 | 필수 |
|------|------|------|
| DATABASE_URL | PostgreSQL 연결 URL | O |
| DATABASE_USERNAME | DB 사용자명 | O |
| DATABASE_PASSWORD | DB 비밀번호 | O |
| REDIS_HOST | Redis 호스트 | O |
| JWT_SECRET | JWT 서명 키 | O |
| GOOGLE_CLIENT_ID | Google OAuth2 | △ |
| KAKAO_CLIENT_ID | Kakao OAuth2 | △ |
| AWS_S3_BUCKET | S3 버킷 | △ |

## 프로젝트 구조

\`\`\`
src/main/java/com/{groupId}/{projectName}/
├── auth/           # 인증/인가
├── common/         # 공통 모듈
├── notification/   # 알림
├── payment/        # 결제
└── Application.java
\`\`\`
```

**Step 2: 커밋**

```bash
git add -A && git commit -m "docs: add README with usage guide"
```

---

## Task 11: 테스트 코드 정리

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

**Step 2: 패키지명 치환**

```bash
find /Users/songseungju/infra_template/src/test/java/com/template/app \
  -name "*.java" -exec sed -i '' 's/com\.cogmo\.annyeong/com.template.app/g' {} \;
```

**Step 3: 도메인 특화 테스트 제거**

feedback, workbook, guardian, question 관련 import/테스트 제거

**Step 4: 테스트 실행**

```bash
cd /Users/songseungju/infra_template && ./gradlew test
```

**Step 5: 커밋**

```bash
git add -A && git commit -m "test: add auth and common module tests"
```

---

## Task 12: 최종 검증 및 정리

**Step 1: 전체 빌드**

```bash
cd /Users/songseungju/infra_template && ./gradlew clean build
```

**Step 2: 불필요한 파일 정리**

- `.gitkeep` 파일 제거
- 빈 디렉토리 정리
- IDE 설정 파일 .gitignore 확인

**Step 3: init-project.sh 테스트**

```bash
# 테스트용 디렉토리에서 실행
cd /tmp
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
- [ ] Task 3: common 모듈 복사
- [ ] Task 4: notification 모듈 복사
- [ ] Task 5: payment 모듈 생성
- [ ] Task 6: 설정 파일 작성
- [ ] Task 7: Docker 설정 파일 작성
- [ ] Task 8: Flyway 마이그레이션 작성
- [ ] Task 9: init-project.sh 작성
- [ ] Task 10: README.md 작성
- [ ] Task 11: 테스트 코드 정리
- [ ] Task 12: 최종 검증 및 정리
