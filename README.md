## Dashboard OAuth Server

Spring Boot OAuth2 authorization server with JWT authentication, MongoDB persistence, and email verification.

## Features

- **Authentication** - Register, login, logout with JWT access tokens and refresh tokens
- **Authorization** - Role-based access control with Users → Roles → Grants model
- **Two-Factor Authentication** - TOTP support for Google Authenticator and similar apps
- **Token Introspection** - External services can validate tokens via `/api/oauth2/introspect`
- **Email Verification** - Scheduled email sending for account verification and password reset via Resend
- **Soft Delete** - All entities support soft delete via `audit.deletedAt`

## Tech Stack

- Spring Boot 3.5.7
- Spring Security with JWT (JJWT)
- MongoDB
- Resend (transactional emails)
- TOTP (dev.samstevens.totp) for 2FA
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
| POST | `/api/auth/verify-email?token=xxx` | Verify email address |
| POST | `/api/auth/forgot-password` | Request password reset email |
| POST | `/api/auth/reset-password` | Reset password with token |

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

### Two-Factor Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/2fa/setup` | Generate TOTP secret and QR code |
| POST | `/api/auth/2fa/verify` | Verify TOTP code and enable 2FA |

## Two-Factor Authentication

TOTP-based 2FA compatible with Google Authenticator, Authy, and similar apps.

### Setup Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. SETUP                                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ POST /api/auth/2fa/setup (with Bearer token)                                 │
│   → Generates TOTP secret                                                    │
│   → Returns { qrCodeDataUri: "data:image/png;base64,...", secret: "..." }   │
│   → Secret stored in user.twoFactorConfig (enabled = false)                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. USER SCANS QR CODE                                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ User scans QR code with authenticator app                                    │
│ App generates 6-digit codes every 30 seconds                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. VERIFY                                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ POST /api/auth/2fa/verify { code: "123456" }                                 │
│   → Validates code against stored secret                                     │
│   → On success: sets enabled = true, returns 200 OK                          │
│   → On failure: returns 400 Bad Request                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Email System

Emails for verification and password reset are sent via a scheduled job using Resend.

### Email Verification Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. REGISTRATION                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ User registers → User created with:                                         │
│   • emailVerified = false                                                   │
│   • emailVerificationToken.token = UUID                                     │
│   • emailVerificationToken.emailSentAt = null                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. SCHEDULER (runs every minute)                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ Finds users where emailVerificationToken.emailSentAt = null                 │
│   → Sends verification email via Resend                                     │
│   → Creates EmailSendAttempt record (SENT or FAILED)                        │
│   → On success: sets emailSentAt = now                                      │
│   → On failure: leaves emailSentAt = null (retried next run)                │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. USER CLICKS EMAIL LINK                                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Frontend calls: POST /api/auth/verify-email?token=xxx                       │
│   → Token validated (exists, not expired, not used)                         │
│   → User updated: emailVerified = true, emailVerificationToken = null       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Password Reset Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. FORGOT PASSWORD REQUEST                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ POST /api/auth/forgot-password { email: "user@example.com" }                │
│   → Creates passwordResetToken with emailSentAt = null                      │
│   → Returns 200 OK (always, to prevent email enumeration)                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. SCHEDULER (runs every minute)                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ Finds users where passwordResetToken.emailSentAt = null                     │
│   → Sends password reset email via Resend                                   │
│   → Creates EmailSendAttempt record                                         │
│   → Updates emailSentAt on success                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. USER CLICKS EMAIL LINK & SUBMITS NEW PASSWORD                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ POST /api/auth/reset-password { token: "xxx", newPassword: "newpass" }      │
│   → Token validated (exists, not expired, not used)                         │
│   → Password updated, passwordResetToken = null                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Configuration

```properties
email.baseUrl=http://localhost:3000
email.fromAddress=Acme <onboarding@resend.dev>
email.verificationTokenExpirationMs=86400000  # 24 hours
email.passwordResetTokenExpirationMs=3600000  # 1 hour
```

## Related Projects

- **Frontend**: [nextjs-dashboard](https://github.com/HugoVrana/dashboard-frontend) - Next.js + Tailwind app
- **Main API**: [spring-dashboard](https://github.com/HugoVrana/dashboard-spring-data) - Spring API for invoice tracking
