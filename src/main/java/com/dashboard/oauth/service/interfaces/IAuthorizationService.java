package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
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
            String state
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
     * Exchanges an authorization code + PKCE code_verifier for tokens.
     * Validates: code exists and unused, code_verifier matches code_challenge,
     * redirect_uri matches, client_id matches.
     */
    AuthResponse exchangeCode(String code, String codeVerifier, String clientId, String redirectUri);
}
