# Spring Boot Service Template

Production-ready Spring Boot 3.x service template with authentication, notifications, payments, and infrastructure components.

## Features

- **Authentication**: JWT + OAuth2 (Google, Kakao, Apple), SMS verification
- **User Management**: Multi-account support (Master/Sub), account lifecycle
- **Notifications**: Email (AWS SES), SMS (NCP SENS), Push (AWS SNS)
- **Payments**: PG integration template (Toss, Iamport compatible)
- **Infrastructure**: Redis caching, Rate limiting, Circuit breaker
- **Observability**: Health checks, Metrics (Prometheus), Swagger/OpenAPI

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.1.11 |
| Database | PostgreSQL 15 |
| Cache | Redis 7 |
| Migration | Flyway |
| Build | Gradle 8.14 |
| Container | Docker, Docker Compose |

## Quick Start

### 1. Initialize New Project

```bash
./init-project.sh my-app com.company.myapp
```

This will:
- Rename package from `com.template.app` to your package
- Update configuration files
- Rename application class

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env with your configuration
```

### 3. Start Development Environment

```bash
# Start PostgreSQL and Redis
docker-compose -f docker-compose.dev.yml up -d

# Run application
./gradlew bootRun
```

### 4. Access Application

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Project Structure

```
src/main/java/com/template/app/
├── auth/                    # Authentication module
│   ├── api/                 # Controllers, DTOs
│   ├── domain/              # Entities
│   ├── infrastructure/      # Security, OAuth2, JWT
│   └── service/             # Business logic
├── common/                  # Shared components
│   ├── config/              # Spring configurations
│   ├── dto/                 # Common DTOs
│   ├── exception/           # Exception handling
│   └── integration/         # External service clients
├── notification/            # Notification module
│   ├── domain/              # Email templates
│   ├── entity/              # EmailLog, PushToken
│   └── service/             # Email, SMS, Push services
└── payment/                 # Payment module
    ├── api/                 # Controllers, DTOs
    ├── domain/              # Payment entity
    └── service/             # Payment service
```

## Configuration

### Environment Variables

See `.env.example` for all available options. Key variables:

| Variable | Description |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Profile: local, dev, prod |
| `DATABASE_URL` | PostgreSQL connection URL |
| `REDIS_HOST` | Redis server host |
| `JWT_SECRET` | JWT signing key (min 32 chars) |
| `AWS_*` | AWS service credentials |
| `NCP_*` | NCP SMS credentials |

### Spring Profiles

| Profile | Usage |
|---------|-------|
| `local` | Local development with relaxed settings |
| `dev` | Development server with full features |
| `prod` | Production with optimized settings |

## API Documentation

### Authentication

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/signup` | POST | User registration |
| `/api/v1/auth/login` | POST | Login with phone/password |
| `/api/v1/auth/refresh` | POST | Refresh access token |
| `/api/v1/auth/oauth2/{provider}` | POST | OAuth2 login |

### Payments

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/payments/init` | POST | Initialize payment |
| `/api/v1/payments/confirm` | POST | Confirm payment |
| `/api/v1/payments/{id}/cancel` | POST | Cancel payment |

## Development

### Running Tests

```bash
./gradlew test
```

### Building Docker Image

```bash
docker build -t my-app .
```

### Full Stack Deployment

```bash
docker-compose up -d
```

## Database Migrations

Migrations are in `src/main/resources/db/migration/`:

| Version | Description |
|---------|-------------|
| V1 | Users table |
| V2 | Auth tables (tokens, attempts, SMS) |
| V3 | Notification tables |
| V4 | Payments table |

To add a new migration:
```bash
touch src/main/resources/db/migration/V5__description.sql
```

## Security

- JWT tokens with configurable expiration
- Password hashing with BCrypt
- Rate limiting on sensitive endpoints
- CORS configuration
- Security headers

## Monitoring

- Health endpoint: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## License

MIT License
