## Dashboard OAuth Server

Spring Boot OAuth2 authorization server with JWT authentication, MongoDB persistence, and email verification.

## Features

- **Authentication** - Register, login, logout with JWT access tokens and refresh tokens
- **Authorization** - Role-based access control with Users → Roles → Grants model
- **OAuth2 Authorization Code Flow** - PKCE-based authorization code flow with full token lifecycle
- **Two-Factor Authentication** - TOTP support for Google Authenticator and similar apps
- **Token Introspection** - External services can validate tokens via introspection endpoints
- **Email Verification** - Scheduled email sending for account verification and password reset via Resend
- **Profile Images** - User profile picture upload and storage via Cloudflare R2
- **Soft Delete** - All entities support soft delete via `audit.deletedAt`

## Tech Stack

- Spring Boot 3.5.7
- Spring Security with JWT (JJWT)
- MongoDB
- Resend (transactional emails)
- TOTP (dev.samstevens.totp) for 2FA
- Cloudflare R2 (S3-compatible profile image storage)
- Lombok

## Getting Started

### Prerequisites

- Java 21+
- MongoDB instance
- Resend API key
- Cloudflare R2 bucket (for profile images)
- Docker (for running integration tests)

### Secrets Management

Secrets are managed via [Doppler](https://doppler.com). `application.properties` contains no secrets — all sensitive values are injected as environment variables at runtime.

**Required secrets:**

| Variable | Description |
|----------|-------------|
| `MONGO_USER` | MongoDB username |
| `MONGO_PASSWORD` | MongoDB password |
| `MONGO_HOST` | MongoDB host (e.g. `cluster.mongodb.net`) |
| `MONGO_DB` | MongoDB database name |
| `JWT_SECRET` | Base64-encoded HMAC secret for JWT signing |
| `SPRING_SECURITY_OAUTH2_SECRET` | Service secret for token introspection |
| `RESEND_APIKEY` | Resend API key for transactional emails |
| `GRAFANA_API_KEY` | Grafana Loki API key |
| `GRAFANA_URL` | Grafana Loki push URL |
| `R2_ACCESS_KEY_ID` | Cloudflare R2 access key |
| `R2_SECRET_ACCESS_KEY` | Cloudflare R2 secret key |
| `R2_ACCOUNT_ID` | Cloudflare account ID |
| `R2_BUCKET_NAME` | R2 bucket name |
| `R2_PUBLIC_URL` | R2 public CDN URL |
| `OIDC_ISSUER` | OIDC issuer URL (e.g. `https://auth.example.com`) |

### Running Locally

**Prerequisites:** [Doppler CLI](https://docs.doppler.com/docs/install-cli) installed and authenticated.

```bash
# First-time setup — select the dashboard-auth-api project and dev_personal config
doppler setup
doppler configure set config dev_personal

# Generate .env.local from Doppler (re-run whenever secrets change)
doppler secrets download --no-file --format env > .env.local
```

**Option A — Terminal:**
```bash
doppler run -- ./mvnw spring-boot:run
```

**Option B — IntelliJ:**

The `.run/DashboardOauthApplication.run.xml` config is already set up to load `.env.local` via the [EnvFile plugin](https://plugins.jetbrains.com/plugin/7861-envfile). Install the plugin, generate `.env.local` as above, then use the normal Run button.

**Without Doppler** (fallback): copy `src/main/resources/application-local.properties`, fill in values manually, and run:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Tests:**
```bash
# Unit tests (no Docker required)
./mvnw test -Dtest="!**.integration.**"

# All tests including integration (requires Docker)
./mvnw test
```

## API Endpoints

### v1 (Deprecated) — Auth (`/api/v1/auth`)

> **Deprecated:** The v1 API is deprecated. Use the v2 endpoints for new integrations.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login, returns JWT + refresh token |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Invalidate refresh token |
| GET | `/api/v1/auth/me` | Get current user info |
| POST | `/api/v1/auth/verify-email` | Verify email address |
| POST | `/api/v1/auth/forgot-password` | Request password reset email |
| POST | `/api/v1/auth/reset-password` | Reset password with token |
| GET | `/api/v1/auth/validate-reset-token` | Validate password reset token |
| POST | `/api/v1/auth/user/role` | Assign role to user |

### v1 (Depracated) — Roles & Grants

> **Deprecated:** The v1 API is deprecated. Use the v2 endpoints for new integrations.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET/POST/DELETE | `/api/v1/role` | Role CRUD |
| GET/POST/DELETE | `/api/v1/grant` | Grant CRUD |

### v1 — Two-Factor Authentication (`/api/v1/auth/2fa`)

> **Deprecated:** The v1 API is deprecated. Use the v2 endpoints for new integrations.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/2fa/setup` | Generate TOTP secret and QR code |
| POST | `/api/v1/auth/2fa/verify` | Verify TOTP code and enable 2FA |

### v1 — User

> **Deprecated:** The v1 API is deprecated. Use the v2 endpoints for new integrations.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/user/me` | Get current user profile |
| PUT | `/api/v1/user/me` | Update current user profile |
| GET | `/api/v1/user/{id}/profilePicture` | Get profile picture |
| POST | `/api/v1/user/profilePicture` | Upload profile picture (multipart/form-data) |

### v1 — Token Introspection

> **Deprecated:** The v1 API is deprecated. Use the v2 endpoints for new integrations.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/oauth2/introspect` | Validate token (requires `X-Service-Secret` header) |

### v2 — Auth

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/auth/register` | Register user (requires `X-Client-Id` header) |

### v2 — OAuth2 Authorization Server (`/v2/oauth2`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v2/oauth2/authorize` | Initiate authorization code flow (PKCE required) |
| POST | `/v2/oauth2/authorize` | Submit credentials |
| POST | `/v2/oauth2/authorize/mfa` | Complete MFA step |
| POST | `/v2/oauth2/token` | Token endpoint (`authorization_code` or `refresh_token` grant) |
| POST | `/v2/oauth2/revoke` | Revoke token (RFC 7009) |
| POST | `/v2/oauth2/introspect` | Token introspection (RFC 7662, Basic auth) |

### v2 — OAuth Clients (`/v2/oauthclients`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v2/oauthclients/{id}` | Get OAuth client |
| POST | `/v2/oauthclients/` | Register new OAuth client |
| DELETE | `/v2/oauthclients/{id}` | Delete OAuth client |
| POST | `/v2/oauthclients/{id}/secret` | Rotate client secret |

### v2 — User Admin (`/api/v2/user`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/user/` | List all users |
| GET | `/api/v2/user/search?q=` | Search users by email |
| GET | `/api/v2/user/{id}` | Get user by ID |
| PUT | `/api/v2/user/{id}` | Update user |
| DELETE | `/api/v2/user/{id}` | Delete user |
| POST | `/api/v2/user/{id}/block` | Block user |
| POST | `/api/v2/user/{id}/unblock` | Unblock user |
| POST | `/api/v2/user/{id}/resend-verification` | Resend verification email |
| POST | `/api/v2/user/{id}/reset-password` | Trigger password reset |

### v2 — Roles & Grants

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/role/` | List all roles |
| GET/POST/DELETE | `/api/v2/role/{id}` | Get/create/delete role |
| GET | `/api/v2/grant/` | List all grants |
| GET/POST/DELETE | `/api/v2/grant/{id}` | Get/create/delete grant |

### v2 — Service (machine-to-machine)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v2/service/grants/ensure` | Idempotently ensure grants exist (Basic auth) |

## Two-Factor Authentication

TOTP-based 2FA compatible with Google Authenticator, Authy, and similar apps.

### Setup Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. SETUP                                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ POST /api/v1/auth/2fa/setup (with Bearer token)                              │
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
│ POST /api/v1/auth/2fa/verify { code: "123456" }                              │
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
│ Frontend calls: POST /api/v1/auth/verify-email?token=xxx                    │
│   → Token validated (exists, not expired, not used)                         │
│   → User updated: emailVerified = true, emailVerificationToken = null       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Password Reset Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. FORGOT PASSWORD REQUEST                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ POST /api/v1/auth/forgot-password { email: "user@example.com" }             │
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
│ POST /api/v1/auth/reset-password { token: "xxx", newPassword: "newpass" }   │
│   → Token validated (exists, not expired, not used)                         │
│   → Password updated, passwordResetToken = null                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Related Projects

- **Frontend**: [nextjs-dashboard](https://github.com/HugoVrana/dashboard-frontend) - Next.js + Tailwind app
- **Main API**: [spring-dashboard](https://github.com/HugoVrana/dashboard-spring-data) - Spring API for invoice tracking
