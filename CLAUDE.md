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

### Database

MongoDB with collections: users, roles, grants, refresh_tokens, email_send_attempts. All entities use soft delete pattern (check `deletedAt != null`).

### External Dependencies

- `com.dashboard:common` - Shared Audit model, custom exceptions (ConflictException, ResourceNotFoundException, InvalidRequestException), GrafanaHttpClient for logging
- JJWT 0.12.3 for JWT operations
- springdoc-openapi for API documentation
- Resend Java SDK for transactional emails

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

- `POST /api/auth/register` - Register user
- `POST /api/auth/login` - Login, returns tokens
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - Invalidate refresh token
- `GET /api/auth/me` - Current user info
- `POST/GET/DELETE /api/auth/role` - Role CRUD
- `POST/GET/DELETE /api/auth/grant` - Grant CRUD
- `POST/DELETE /api/auth/role/grant` - Assign/remove grants from roles
- `POST /api/auth/user/role` - Assign role to user
- `POST /api/oauth2/introspect` - Token introspection (requires X-Service-Secret)
- `POST /api/user/{userId}/profilePicture` - Upload profile picture (multipart/form-data)

## Patterns to Follow

- All services have interfaces (IAuthenticationService, etc.)
- Constructor injection via Lombok's @RequiredArgsConstructor
- Map entities to DTOs through mapper interfaces
- Global exception handling via GlobalExceptionHandler with ProblemDetail responses
- Check for soft-deleted entities (deletedAt != null) in queries
