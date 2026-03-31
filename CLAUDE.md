# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build
./mvnw clean package

# Build (skip tests)
./mvnw clean package -DskipTests

# Run locally (port 8081)
./mvnw spring-boot:run

# Run tests (requires Docker for integration tests)
./mvnw test

# Run single test class
./mvnw test -Dtest=ClassName

# Run unit tests only (no Docker required)
./mvnw test -Dtest="!*IntegrationTest"
```

**Note:** Integration tests use Testcontainers to spin up MongoDB, which requires Docker to be running.

## Architecture Overview

Spring Boot 3.5.7 OAuth2 authorization server with JWT authentication and MongoDB persistence.

### Core Flow
1. **Register** → Create user with email/password/roleId
2. **Login** → Returns JWT access token + refresh token (stored in MongoDB)
3. **Protected requests** → JwtAuthFilter validates Bearer token, sets SecurityContext
4. **Refresh** → Exchange refresh token for new access token
5. **Introspection** → External services validate tokens via `/api/oauth2/introspect` (requires X-Service-Secret header)

### Key Packages

- `config/` - SecurityConfig (JWT filter chain, BCrypt encoder), CorsConfig, ResendConfig
- `controller/` - AuthenticationController (`/api/auth/*`), TokenController (`/api/oauth2/introspect`)
- `service/` - AuthenticationService, JwtService (JJWT library), UserService, RoleService, GrantService, EmailService, EmailSenderService
- `filter/JwtAuthFilter` - Extracts JWT from Authorization header, validates, sets authentication
- `model/entities/` - User, Role, Grant, RefreshToken, VerificationToken, EmailSendAttempt (all with soft delete via Audit.deletedAt)
- `model/enums/` - EmailType (VERIFICATION, PASSWORD_RESET), EmailSendStatus (QUEUED, SENT, DELIVERED, BOUNCED, FAILED)
- `mapper/` - Interface-based mappers (IUserInfoMapper, IRoleMapper, IGrantMapper)
- `dataTransferObject/` - Request/response DTOs for auth, role, grant, user operations
- `scheduler/` - EmailScheduler (scheduled email sending)

### Authorization Model

- Users → Roles → Grants (many-to-many via DBRef)
- JWT claims include userId, email (subject), and flattened grants list
- Roles become GrantedAuthorities as "ROLE_[name]"

### Email System

Scheduled email sending for verification and password reset emails via Resend API.

**Architecture:**
```
EmailScheduler (triggers every minute)
    ↓
EmailService (orchestrates)
    ├── Fetches users with unsent emails (IUserRepository)
    ├── Sends via EmailSenderService (Resend API)
    ├── Records EmailSendAttempt for history/audit
    └── Updates VerificationToken.emailSentAt on success
```

**Key Classes:**
- `EmailScheduler` - Thin scheduler, just triggers `EmailService` methods
- `EmailService` - Orchestrates: fetch users → send email → record attempt → update token
- `EmailSenderService` - Low-level Resend API wrapper, returns message ID
- `VerificationToken` - Embedded in User for emailVerificationToken and passwordResetToken
- `EmailSendAttempt` - Separate collection for send history/audit trail

**Flow:**
1. User registers/requests reset → `VerificationToken` created with `emailSentAt=null`
2. Scheduler runs → finds users with unsent emails
3. For each user: create attempt → send → update status → save attempt
4. On failure: attempt marked FAILED, `emailSentAt` stays null for retry on next run

### Profile Image Storage (Cloudflare R2)

User profile images are stored in Cloudflare R2 (S3-compatible).

**Architecture:**
- `R2Properties` - Configuration (accountId, bucketName, accessKeyId, secretAccessKey)
- `R2Config` - Creates S3Client bean configured for R2 endpoint
- `R2Service` - Uploads/deletes files from R2, returns public URL and r2Key
- `UserController.setUserProfilePicture` - Endpoint for uploading profile pictures

**User fields:**
- `profileImageUrl` - Public URL to display the image
- `profileImageR2Key` - R2 key for deletion when updating

**Flow:**
1. User uploads image via `POST /api/user/{userId}/profilePicture`
2. Old image deleted from R2 (if exists)
3. New image uploaded, URL and key saved to User document
4. Profile image URL returned in login/refresh/me responses

### Two-Factor Authentication (TOTP)

Time-based One-Time Password (TOTP) support for Google Authenticator and similar apps.

**Architecture:**
- `BaseTwoFactorConfig` - Abstract base class for 2FA methods (enabled, audit)
- `TotpConfig` - Extends base, stores TOTP secret
- `TotpService` - Generates secrets, QR codes, and verifies codes
- `TotpController` - Exposes `/api/auth/2fa/*` endpoints

**User field:**
- `twoFactorConfig` - Stores the active 2FA configuration (polymorphic)

**Flow:**
1. User calls `POST /api/auth/2fa/setup` → generates secret, returns QR code data URI
2. User scans QR code with authenticator app
3. User calls `POST /api/auth/2fa/verify` with 6-digit code → validates and enables 2FA

### Database

MongoDB with collections: users, roles, grants, refresh_tokens, email_send_attempts. All entities use soft delete pattern (check `deletedAt != null`).

### External Dependencies

- `com.dashboard:common` - Shared Audit model, custom exceptions (ConflictException, ResourceNotFoundException, InvalidRequestException), GrafanaHttpClient for logging
- JJWT 0.12.3 for JWT operations
- springdoc-openapi for API documentation
- Resend Java SDK for transactional emails
- totp 1.7.1 (dev.samstevens.totp) for TOTP 2FA

### Configuration Properties

Key properties in `application.properties`:
- `JWT.SECRET` - BASE64 encoded secret for HMAC signing
- `JWT.EXPIRATION` - Token expiration in milliseconds (default 86400000 = 24h)
- `spring.security.oauth2.secret` - Service secret for token introspection endpoint
- `spring.data.mongodb.uri` - MongoDB Atlas connection string
- `resend.api-key` - Resend API key for sending emails
- `r2.accessKeyId` - Cloudflare R2 access key
- `r2.secretAccessKey` - Cloudflare R2 secret key
- `r2.accountId` - Cloudflare account ID
- `r2.bucketName` - R2 bucket name

## API Endpoints

### v1 (`/api/v1/`)

**Auth** (`/api/v1/auth`)
- `POST /api/v1/auth/register` - Register user
- `POST /api/v1/auth/login` - Login, returns tokens
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - Invalidate refresh token
- `GET /api/v1/auth/me` - Current user info
- `POST /api/v1/auth/verify-email` - Verify email address
- `POST /api/v1/auth/forgot-password` - Request password reset
- `POST /api/v1/auth/reset-password` - Reset password
- `GET /api/v1/auth/validate-reset-token` - Validate password reset token
- `POST /api/v1/auth/user/role` - Assign role to user

**Roles** (`/api/v1/role`)
- `GET/POST/DELETE /api/v1/role` - Role CRUD

**Grants** (`/api/v1/grant`)
- `GET/POST/DELETE /api/v1/grant` - Grant CRUD

**2FA / TOTP** (`/api/v1/auth/2fa`)
- `POST /api/v1/auth/2fa/setup` - Generate TOTP secret + QR code
- `POST /api/v1/auth/2fa/verify` - Verify TOTP code and enable 2FA

**User** (`/api/v1/user`)
- `GET /api/v1/user/me` - Current user profile
- `PUT /api/v1/user/me` - Update current user profile
- `GET /api/v1/user/{id}/profilePicture` - Get profile picture
- `POST /api/v1/user/profilePicture` - Upload profile picture (multipart/form-data)

**OAuth2** (`/api/v1/oauth2`)
- `POST /api/v1/oauth2/introspect` - Token introspection (requires X-Service-Secret)

**Activity** (`/api/v1/activity`)
- `GET /api/v1/activity/recent` - Recent activity

### v2 (`/v2/` or `/api/v2/`)

**Auth** (`/api/v2/auth`)
- `POST /api/v2/auth/register` - Register user (requires `X-Client-Id` header)

**OAuth2 Authorization Server** (`/v2/oauth2`)
- `GET /v2/oauth2/authorize` - Initiate authorization code flow (PKCE required)
- `POST /v2/oauth2/authorize` - Submit credentials (form-urlencoded)
- `POST /v2/oauth2/authorize/mfa` - Complete MFA step (form-urlencoded)
- `POST /v2/oauth2/token` - Token endpoint: `authorization_code` or `refresh_token` grant (form-urlencoded)
- `POST /v2/oauth2/revoke` - Revoke token (RFC 7009, form-urlencoded)
- `POST /v2/oauth2/introspect` - Token introspection (RFC 7662, Basic auth)

**OAuth Clients** (`/v2/oauthclients`)
- `GET /v2/oauthclients/{id}` - Get OAuth client
- `POST /v2/oauthclients/` - Register new OAuth client (requires `dashboard-oauth-client-create`)
- `DELETE /v2/oauthclients/{id}` - Delete OAuth client (requires `dashboard-oauth-client-delete`)
- `POST /v2/oauthclients/{id}/secret` - Rotate client secret (requires `dashboard-oauth-client-rotate-secret`)

**Service (machine-to-machine)** (`/v2/service`)
- `POST /v2/service/grants/ensure` - Idempotently ensure grants exist (Basic auth with service secret)

**Users Admin** (`/api/v2/user`)
- `GET /api/v2/user/` - List all users
- `GET /api/v2/user/search?q=` - Search users by email
- `GET /api/v2/user/{id}` - Get user by ID
- `PUT /api/v2/user/{id}` - Update user (requires `dashboard-oauth-user-update`)
- `DELETE /api/v2/user/{id}` - Delete user (requires `dashboard-oauth-user-delete`)
- `POST /api/v2/user/{id}/block` - Block user (requires `dashboard-oauth-user-block`)
- `POST /api/v2/user/{id}/unblock` - Unblock user (requires `dashboard-oauth-user-block`)
- `POST /api/v2/user/{id}/resend-verification` - Resend verification email (requires `dashboard-oauth-user-resend-verification`)
- `POST /api/v2/user/{id}/reset-password` - Trigger password reset (requires `dashboard-oauth-user-reset-password`)

**Roles** (`/api/v2/role`)
- `GET /api/v2/role/` - List all roles
- `GET /api/v2/role/{id}` - Get role by ID
- `POST /api/v2/role/` - Create role (requires `dashboard-oauth-role-create`)
- `DELETE /api/v2/role/{id}` - Delete role (requires `dashboard-oauth-role-delete`)

**Grants** (`/api/v2/grant`)
- `GET /api/v2/grant/` - List all grants
- `GET /api/v2/grant/{id}` - Get grant by ID
- `POST /api/v2/grant/` - Create grant (requires `dashboard-oauth-grant-create`)
- `DELETE /api/v2/grant/{id}` - Delete grant (requires `dashboard-oauth-grant-delete`)

## Patterns to Follow

- All services have interfaces (IAuthenticationService, etc.)
- Constructor injection via Lombok's @RequiredArgsConstructor
- Map entities to DTOs through mapper interfaces
- Global exception handling via GlobalExceptionHandler with ProblemDetail responses
- Check for soft-deleted entities (deletedAt != null) in queries
