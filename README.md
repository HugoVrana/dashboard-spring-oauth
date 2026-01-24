## Dashboard OAuth Server

Spring Boot OAuth2 authorization server with JWT authentication, MongoDB persistence, and email verification.

## Features

- **Authentication** - Register, login, logout with JWT access tokens and refresh tokens
- **Authorization** - Role-based access control with Users → Roles → Grants model
- **Token Introspection** - External services can validate tokens via `/api/oauth2/introspect`
- **Email Verification** - Scheduled email sending for account verification and password reset via Resend
- **Soft Delete** - All entities support soft delete via `audit.deletedAt`

## Tech Stack

- Spring Boot 3.5.7
- Spring Security with JWT (JJWT)
- MongoDB
- Resend (transactional emails)
- Lombok

## Getting Started

### Prerequisites

- Java 21+
- MongoDB instance
- Resend API key

### Configuration

Set the following properties in `application.properties` or environment variables:

```properties
spring.data.mongodb.uri=<mongodb-connection-string>
JWT.SECRET=<base64-encoded-secret>
JWT.EXPIRATION=86400000
spring.security.oauth2.secret=<service-secret>
resend.api-key=<resend-api-key>
```

### Running Locally

```bash
# Build
./mvnw clean package

# Run (port 8081)
./mvnw spring-boot:run

# Run tests
./mvnw test
```

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, returns JWT + refresh token |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Invalidate refresh token |
| GET | `/api/auth/me` | Get current user info |

### Roles & Grants
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST/GET/DELETE | `/api/auth/role` | Role CRUD |
| POST/GET/DELETE | `/api/auth/grant` | Grant CRUD |
| POST/DELETE | `/api/auth/role/grant` | Assign/remove grants from roles |
| POST | `/api/auth/user/role` | Assign role to user |

### Token Introspection
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/oauth2/introspect` | Validate token (requires X-Service-Secret header) |

## Email System

Emails for verification and password reset are sent via a scheduled job:

1. User registers → `VerificationToken` created with `emailSentAt=null`
2. Scheduler runs every minute → finds users with unsent emails
3. Sends email via Resend → records `EmailSendAttempt` → updates `emailSentAt`
4. On failure → attempt marked FAILED, retried on next scheduler run

## Related Projects

- **Frontend**: [nextjs-dashboard](https://gitlab.com/hugo.vrana/nextjs-dashboard) - Next.js + Tailwind app
- **Main API**: [spring-dashboard](https://gitlab.com/hugo.vrana/spring-dashboard) - Spring API for invoice tracking
