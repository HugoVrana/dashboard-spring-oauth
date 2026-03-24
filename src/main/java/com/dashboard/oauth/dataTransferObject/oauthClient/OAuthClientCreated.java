package com.dashboard.oauth.dataTransferObject.oauthClient;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Returned only on client creation. Contains the raw clientSecret — shown once, never again.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OAuthClientCreated extends OAuthClientRead {
    private String clientSecret;
}
