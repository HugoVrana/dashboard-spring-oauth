package com.dashboard.oauth.dataTransferObject.v2;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
public class AuthorizeRequest {

    @Parameter(description = "Must be `code`", required = true)
    private String responseType;

    @Parameter(description = "Client identifier", required = true)
    private String clientId;

    @Parameter(description = "Registered redirect URI", required = true)
    private String redirectUri;

    @Parameter(description = "PKCE code challenge (RFC 7636)", required = true)
    private String codeChallenge;

    @Parameter(description = "Must be `S256`", required = true)
    private String codeChallengeMethod;

    @Parameter(description = "Space-separated list of requested scopes")
    private String scope;

    @Parameter(description = "Opaque value for CSRF protection, echoed back on redirect")
    private String state;

    @Parameter(description = "OpenID Connect nonce for id_token replay protection")
    private String nonce;
}
