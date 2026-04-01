package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.v2.IntrospectionResponse;
import com.dashboard.oauth.dataTransferObject.v2.SubmitAuthorizeResult;
import com.dashboard.oauth.model.entities.AuthorizationCode;
import com.dashboard.oauth.model.entities.AuthorizationRequest;

public interface IAuthorizationService {

    /**
     * Validates the authorization request params (client, redirect URI, PKCE) and
     * stores a pending AuthorizationRequest. Returns the stored request so the caller
     * can redirect the user to the login page with the request ID.
     */
    AuthorizationRequest createAuthorizationRequest(
            String clientId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String scope,
            String state,
            String nonce
    );

    /**
     * Loads a pending (unused, non-expired) AuthorizationRequest by its ID.
     * Throws InvalidRequestException if not found or already used.
     */
    AuthorizationRequest getAuthorizationRequest(String requestId);

    /**
     * Generates a one-time AuthorizationCode for the given user and marks the
     * AuthorizationRequest as used.
     */
    AuthorizationCode createAuthorizationCode(AuthorizationRequest request, String userId);

    /**
     * Creates a short-lived MFA token that ties a validated user identity to a
     * pending AuthorizationRequest. Returned to the client when 2FA is required.
     */
    String createMfaToken(String userId, AuthorizationRequest request);

    /**
     * Verifies the TOTP code against the user referenced by the MFA token. Only
     * if the code is valid does it consume the token and create an AuthorizationCode.
     * Throws InvalidRequestException if the MFA token is invalid/expired/already used.
     * Returns null if the TOTP code itself is wrong (token stays valid for retry).
     */
    AuthorizationCode exchangeMfaToken(String mfaToken, String totpCode);

    /**
     * Exchanges an authorization code + PKCE code_verifier for tokens.
     * Validates: code exists and unused, code_verifier matches code_challenge,
     * redirect_uri matches, client_id matches. If the client has a stored secret,
     * clientSecret must be provided and must match.
     */
    AuthResponse exchangeCode(String code, String codeVerifier, String clientId, String redirectUri, String clientSecret);

    /**
     * Validates user credentials and 2FA state for a pending authorization request.
     * Returns a result indicating whether an MFA challenge or an authorization code was issued.
     * Throws BadCredentialsException or LockedException on invalid/locked credentials.
     */
    SubmitAuthorizeResult submitAuthorize(AuthorizationRequest authRequest, String username, String password);

    /**
     * Validates a Bearer token and returns its introspection metadata.
     * Always returns a response — inactive if the token is invalid, expired, or the user/grants no longer exist.
     */
    IntrospectionResponse introspect(String token);

    /**
     * Validates the HTTP Basic authorization header against the configured service client secret.
     */
    boolean validateClientSecret(String authorizationHeader);
}
