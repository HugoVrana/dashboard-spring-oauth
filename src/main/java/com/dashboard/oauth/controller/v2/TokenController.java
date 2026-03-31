package com.dashboard.oauth.controller.v2;

import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.v2.IntrospectionResponse;
import com.dashboard.oauth.dataTransferObject.v2.MfaRequiredResponse;
import com.dashboard.oauth.dataTransferObject.v2.OAuth2ErrorResponse;
import com.dashboard.oauth.dataTransferObject.v2.SubmitAuthorizeResult;
import com.dashboard.oauth.dataTransferObject.v2.TokenResponse;
import com.dashboard.oauth.environment.Oauth2Properties;
import com.dashboard.oauth.mapper.interfaces.ITokenResponseMapper;
import com.dashboard.oauth.model.entities.AuthorizationRequest;
import com.dashboard.oauth.service.interfaces.IAuthorizationService;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

@RestController
@CrossOrigin
@RequestMapping("/v2/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "OAuth 2.0 authorization server endpoints (RFC 6749, RFC 7009, RFC 7662)")
public class TokenController {

    private final IAuthorizationService authorizationService;
    private final IAuthenticationService authenticationService;
    private final IOAuthClientService oAuthClientService;
    private final ITokenResponseMapper tokenResponseMapper;
    private final Oauth2Properties oauth2Properties;

    // -------------------------------------------------------------------------
    // GET /v2/oauth2/authorize
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Authorization endpoint — initiate (RFC 6749 §4.1.1)",
            description = "Validates the authorization request and redirects the user-agent to the login page. " +
                    "Only `response_type=code` is supported. PKCE (`code_challenge` + `code_challenge_method`) is required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to login page (success) or redirect_uri with error",
                    headers = @Header(name = "Location", description = "Login URL with request_id, or redirect_uri with error params"))
    })
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(
            @Parameter(description = "Must be `code`", required = true) @RequestParam("response_type") String responseType,
            @Parameter(description = "Client identifier", required = true) @RequestParam("client_id") String clientId,
            @Parameter(description = "Registered redirect URI", required = true) @RequestParam("redirect_uri") String redirectUri,
            @Parameter(description = "PKCE code challenge (RFC 7636)", required = true) @RequestParam("code_challenge") String codeChallenge,
            @Parameter(description = "Must be `S256`", required = true) @RequestParam("code_challenge_method") String codeChallengeMethod,
            @Parameter(description = "Space-separated list of requested scopes") @RequestParam(value = "scope", required = false) String scope,
            @Parameter(description = "Opaque value for CSRF protection, echoed back on redirect") @RequestParam(value = "state", required = false) String state,
            @Parameter(description = "OpenID Connect nonce for id_token replay protection") @RequestParam(value = "nonce", required = false) String nonce,
            HttpServletRequest httpRequest) {

        if (!"code".equals(responseType)) {
            return buildErrorRedirect(redirectUri, "unsupported_response_type",
                    "Only response_type=code is supported", state);
        }

        if (!oAuthClientService.isRegisteredClient(clientId)) {
            return buildErrorRedirect(redirectUri, "unauthorized_client",
                    "Unknown client_id", state);
        }

        if (!oAuthClientService.isAllowedHost(clientId, httpRequest)) {
            return buildErrorRedirect(redirectUri, "access_denied",
                    "The request origin is not allowed for this client", state);
        }

        try {
            AuthorizationRequest request = authorizationService.createAuthorizationRequest(
                    clientId, redirectUri, codeChallenge, codeChallengeMethod, scope, state, nonce);

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
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Authorization endpoint — submit credentials",
            description = "Called by the login form. Validates credentials and either issues an authorization code " +
                    "redirect or returns an `mfa_required` response if 2FA is enabled on the account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to redirect_uri with authorization code",
                    headers = @Header(name = "Location", description = "redirect_uri?code=...&state=...")),
            @ApiResponse(responseCode = "200", description = "MFA required — contains mfa_token for the next step",
                    content = @Content(schema = @Schema(implementation = MfaRequiredResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request_id",
                    content = @Content(schema = @Schema(implementation = OAuth2ErrorResponse.class)))
    })
    @PostMapping(value = "/authorize", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> submitAuthorize(
            @Parameter(description = "Authorization request ID from the GET /authorize redirect", required = true) @RequestParam("request_id") String requestId,
            @Parameter(description = "User email", required = true) @RequestParam("username") String username,
            @Parameter(description = "User password", required = true) @RequestParam("password") String password,
            HttpServletRequest httpRequest) {

        AuthorizationRequest authRequest;
        try {
            authRequest = authorizationService.getAuthorizationRequest(requestId);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("invalid_request", e.getMessage()));
        }

        if (!oAuthClientService.isAllowedHost(authRequest.getClientId(), httpRequest)) {
            return buildErrorRedirect(authRequest.getRedirectUri(), "access_denied",
                    "The request origin is not allowed for this client", authRequest.getState());
        }

        SubmitAuthorizeResult result;
        try {
            result = authorizationService.submitAuthorize(authRequest, username, password);
        } catch (BadCredentialsException | org.springframework.security.authentication.LockedException e) {
            return buildErrorRedirect(authRequest.getRedirectUri(), "access_denied",
                    "Invalid credentials", authRequest.getState());
        }

        if (result.mfaRequired()) {
            return ResponseEntity.ok(new MfaRequiredResponse(true, result.mfaToken()));
        }

        URI location = buildRedirectUri(
                result.authorizationCode().getRedirectUri(),
                result.authorizationCode().getCode(),
                result.authorizationCode().getState());
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/authorize/mfa
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Authorization endpoint — complete MFA step",
            description = "Called after receiving `mfa_required=true`. Validates the TOTP code and completes the authorization code redirect."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "MFA valid — redirect to redirect_uri with authorization code",
                    headers = @Header(name = "Location", description = "redirect_uri?code=...&state=...")),
            @ApiResponse(responseCode = "401", description = "Invalid TOTP code",
                    content = @Content(schema = @Schema(implementation = OAuth2ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired mfa_token",
                    content = @Content(schema = @Schema(implementation = OAuth2ErrorResponse.class)))
    })
    @PostMapping(value = "/authorize/mfa", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> submitMfa(
            @Parameter(description = "MFA token received from POST /authorize", required = true) @RequestParam("mfa_token") String mfaToken,
            @Parameter(description = "6-digit TOTP code from authenticator app", required = true) @RequestParam("totp_code") String totpCode) {

        try {
            var authCode = authorizationService.exchangeMfaToken(mfaToken, totpCode);
            if (authCode == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new OAuth2ErrorResponse("invalid_grant", "Invalid TOTP code"));
            }
            URI redirectUri = buildRedirectUri(authCode.getRedirectUri(), authCode.getCode(), authCode.getState());
            return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("invalid_request", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/token
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Token endpoint (RFC 6749 §3.2)",
            description = "Issues access and refresh tokens. Supports `authorization_code` (with PKCE) and `refresh_token` grant types. " +
                    "Responses include `Cache-Control: no-store` and `Pragma: no-cache` per RFC 6749 §5.1."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens issued",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid grant or missing parameters",
                    content = @Content(schema = @Schema(implementation = OAuth2ErrorResponse.class)))
    })
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> token(
            @Parameter(description = "`authorization_code` or `refresh_token`", required = true) @RequestParam("grant_type") String grantType,
            @Parameter(description = "Authorization code (required for `authorization_code` grant)") @RequestParam(value = "code", required = false) String code,
            @Parameter(description = "PKCE code verifier (required for `authorization_code` grant)") @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @Parameter(description = "Client ID (required for `authorization_code` grant)") @RequestParam(value = "client_id", required = false) String clientId,
            @Parameter(description = "Redirect URI (required for `authorization_code` grant)") @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @Parameter(description = "Client secret (required for confidential clients)") @RequestParam(value = "client_secret", required = false) String clientSecret,
            @Parameter(description = "Refresh token (required for `refresh_token` grant)") @RequestParam(value = "refresh_token", required = false) String refreshToken) {

        ResponseEntity<?> response = switch (grantType) {
            case "authorization_code" -> handleAuthorizationCodeGrant(code, codeVerifier, clientId, redirectUri, clientSecret);
            case "refresh_token" -> handleRefreshTokenGrant(refreshToken);
            default -> ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("unsupported_grant_type", null));
        };
        return ResponseEntity.status(response.getStatusCode())
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(response.getBody());
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/revoke  (RFC 7009)
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Token revocation endpoint (RFC 7009)",
            description = "Revokes an access or refresh token. Always returns 200 even if the token is unknown or already revoked."
    )
    @ApiResponse(responseCode = "200", description = "Token revoked (or was already invalid)")
    @PostMapping(value = "/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> revoke(
            @Parameter(description = "The token to revoke", required = true) @RequestParam("token") String token,
            @Parameter(description = "Hint about the token type: `access_token` or `refresh_token`") @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint) {
        authenticationService.revokeToken(token);
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/introspect  (RFC 7662)
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Token introspection endpoint (RFC 7662)",
            description = "Allows resource servers to validate a token and retrieve its metadata. " +
                    "Requires HTTP Basic authentication using the service client secret."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Introspection result (active or inactive)",
                    content = @Content(schema = @Schema(implementation = IntrospectionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid Basic auth credentials", content = @Content)
    })
    @PostMapping(value = "/introspect", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> introspect(
            @Parameter(description = "The token to introspect", required = true) @RequestParam("token") String token,
            @Parameter(description = "HTTP Basic credentials (Base64 encoded `client_id:client_secret`)", required = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!authorizationService.validateClientSecret(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"oauth2\"")
                    .build();
        }

        return ResponseEntity.ok(authorizationService.introspect(token));
    }

    // -------------------------------------------------------------------------
    // Private HTTP helpers
    // -------------------------------------------------------------------------

    private ResponseEntity<?> handleAuthorizationCodeGrant(
            String code, String codeVerifier, String clientId, String redirectUri, String clientSecret) {
        if (code == null || codeVerifier == null || clientId == null || redirectUri == null) {
            return ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("invalid_request",
                            "code, code_verifier, client_id and redirect_uri are required"));
        }
        try {
            AuthResponse authResponse = authorizationService.exchangeCode(code, codeVerifier, clientId, redirectUri, clientSecret);
            return ResponseEntity.ok(tokenResponseMapper.toTokenResponse(authResponse));
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
            return ResponseEntity.ok(tokenResponseMapper.toTokenResponse(authResponse));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new OAuth2ErrorResponse("invalid_grant", "Invalid or expired refresh token"));
        }
    }

    private URI buildRedirectUri(String baseUri, String code, String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUri)
                .queryParam("code", code);
        if (state != null) {
            builder.queryParam("state", state);
        }
        return builder.build().toUri();
    }

    private ResponseEntity<?> buildErrorRedirect(String redirectUri, String error, String description, String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", error);
        if (description != null) {
            builder.queryParam("error_description", description);
        }
        if (state != null) {
            builder.queryParam("state", state);
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(builder.build().toUri()).build();
    }
}
