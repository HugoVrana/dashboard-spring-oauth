package com.dashboard.oauth.controller.v2;

import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.v2.IntrospectionResponse;
import com.dashboard.oauth.dataTransferObject.v2.OAuth2ErrorResponse;
import com.dashboard.oauth.dataTransferObject.v2.TokenResponse;
import com.dashboard.oauth.environment.Oauth2Properties;
import com.dashboard.oauth.model.entities.AuthorizationCode;
import com.dashboard.oauth.model.entities.AuthorizationRequest;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.interfaces.IAuthorizationService;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.dashboard.oauth.service.interfaces.IJwtService;
import com.dashboard.oauth.service.interfaces.IUserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/v2/oauth2")
@RequiredArgsConstructor
public class TokenController {

    private final IAuthorizationService authorizationService;
    private final IAuthenticationService authenticationService;
    private final IUserService userService;
    private final IJwtService jwtService;
    private final IGrantService grantService;
    private final Oauth2Properties oauth2Properties;

    // -------------------------------------------------------------------------
    // GET /v2/oauth2/authorize
    // Validates the authorization request, persists it, redirects to the React
    // login page with a request_id so the user can complete authentication.
    // -------------------------------------------------------------------------
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam("code_challenge_method") String codeChallengeMethod,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state) {

        if (!"code".equals(responseType)) {
            return buildErrorRedirect(redirectUri, "unsupported_response_type",
                    "Only response_type=code is supported", state);
        }

        try {
            AuthorizationRequest request = authorizationService.createAuthorizationRequest(
                    clientId, redirectUri, codeChallenge, codeChallengeMethod, scope, state);

            String loginUrl = oauth2Properties.getLoginUrl()
                    + "?request_id=" + request.getId().toHexString();
            if (state != null) {
                loginUrl += "&state=" + state;
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(loginUrl))
                    .build();

        } catch (InvalidRequestException e) {
            return buildErrorRedirect(redirectUri, "invalid_request", e.getMessage(), state);
        }
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/authorize
    // Called by the React login form. Validates credentials, generates an auth
    // code, and redirects back to the client's redirect_uri with the code.
    // -------------------------------------------------------------------------
    @PostMapping(value = "/authorize", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> submitAuthorize(
            @RequestParam("request_id") String requestId,
            @RequestParam("username") String username,
            @RequestParam("password") String password) {

        AuthorizationRequest authRequest;
        try {
            authRequest = authorizationService.getAuthorizationRequest(requestId);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("invalid_request", e.getMessage()));
        }

        User user;
        try {
            user = userService.getUserByEmail(username)
                    .filter(u -> u.getAudit().getDeletedAt() == null)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            // Delegate credential check to the existing login flow via a LoginRequest
            com.dashboard.oauth.dataTransferObject.auth.LoginRequest loginRequest =
                    new com.dashboard.oauth.dataTransferObject.auth.LoginRequest();
            loginRequest.setEmail(username);
            loginRequest.setPassword(password);
            // This validates credentials and handles locking/attempt counting
            authenticationService.login(loginRequest);

        } catch (BadCredentialsException | org.springframework.security.authentication.LockedException e) {
            return buildErrorRedirect(authRequest.getRedirectUri(), "access_denied",
                    "Invalid credentials", authRequest.getState());
        }

        AuthorizationCode code = authorizationService.createAuthorizationCode(
                authRequest, user.get_id().toHexString());

        String redirectUrl = authRequest.getRedirectUri() + "?code=" + code.getCode();
        if (authRequest.getState() != null) {
            redirectUrl += "&state=" + authRequest.getState();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/token
    // Supports: authorization_code, refresh_token
    // -------------------------------------------------------------------------
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "refresh_token", required = false) String refreshToken) {

        return switch (grantType) {
            case "authorization_code" -> handleAuthorizationCodeGrant(code, codeVerifier, clientId, redirectUri);
            case "refresh_token" -> handleRefreshTokenGrant(refreshToken);
            default -> ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("unsupported_grant_type", null));
        };
    }

    private ResponseEntity<?> handleAuthorizationCodeGrant(
            String code, String codeVerifier, String clientId, String redirectUri) {

        if (code == null || codeVerifier == null || clientId == null || redirectUri == null) {
            return ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("invalid_request",
                            "code, code_verifier, client_id and redirect_uri are required"));
        }
        try {
            AuthResponse authResponse = authorizationService.exchangeCode(
                    code, codeVerifier, clientId, redirectUri);
            return ResponseEntity.ok(mapToTokenResponse(authResponse));
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("invalid_grant", e.getMessage()));
        }
    }

    private ResponseEntity<?> handleRefreshTokenGrant(String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("invalid_request", "refresh_token is required"));
        }
        try {
            AuthResponse authResponse = authenticationService.refreshToken(refreshToken);
            return ResponseEntity.ok(mapToTokenResponse(authResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("invalid_grant", "Invalid or expired refresh token"));
        }
    }

    private TokenResponse mapToTokenResponse(AuthResponse authResponse) {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(authResponse.getAccessToken());
        tokenResponse.setExpiresIn(authResponse.getExpiresIn() / 1000);
        tokenResponse.setRefreshToken(authResponse.getRefreshToken());
        return tokenResponse;
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/introspect  (unchanged)
    // -------------------------------------------------------------------------
    @PostMapping(value = "/introspect", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> introspect(
            @RequestParam("token") String token,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"oauth2\"")
                    .build();
        }

        try {
            List<String> grantNames = jwtService.extractClaim(token, c ->
                    ((List<?>) c.get("grants")).stream()
                            .map(String::valueOf)
                            .toList()
            );
            String email = jwtService.extractUsername(token);
            Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

            Optional<User> optionalUser = userService.getUserByEmail(email);
            if (optionalUser.isEmpty() || optionalUser.get().getAudit().getDeletedAt() != null) {
                IntrospectionResponse inactive = new IntrospectionResponse();
                inactive.setActive(false);
                return ResponseEntity.ok(inactive);
            }

            List<String> activeGrants = new ArrayList<>();
            for (String grantName : grantNames) {
                Optional<Grant> optional = grantService.getGrantByName(grantName);
                if (optional.isEmpty() || optional.get().getAudit().getDeletedAt() != null) {
                    IntrospectionResponse inactive = new IntrospectionResponse();
                    inactive.setActive(false);
                    return ResponseEntity.ok(inactive);
                }
                activeGrants.add(grantName);
            }

            IntrospectionResponse response = new IntrospectionResponse();
            response.setActive(true);
            response.setSub(email);
            response.setExp(expiration.toInstant().getEpochSecond());
            response.setScope(String.join(" ", activeGrants));
            return ResponseEntity.ok(response);

        } catch (JwtException e) {
            IntrospectionResponse inactive = new IntrospectionResponse();
            inactive.setActive(false);
            return ResponseEntity.ok(inactive);
        }
    }

    private boolean isAuthorized(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
            String clientSecret = decoded.contains(":") ? decoded.substring(decoded.indexOf(':') + 1) : decoded;
            return MessageDigest.isEqual(
                    oauth2Properties.getSecret().getBytes(StandardCharsets.UTF_8),
                    clientSecret.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private ResponseEntity<?> buildErrorRedirect(String redirectUri, String error, String description, String state) {
        String url = redirectUri + "?error=" + error
                + (description != null ? "&error_description=" + description : "")
                + (state != null ? "&state=" + state : "");
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }
}
