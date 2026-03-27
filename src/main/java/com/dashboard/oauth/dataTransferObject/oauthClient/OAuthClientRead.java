package com.dashboard.oauth.dataTransferObject.oauthClient;

import lombok.Data;

import java.util.List;

@Data
public class OAuthClientRead {
    private String id;
    private List<String> redirectUris;
    private List<String> allowedHosts;
    private List<String> allowedScopes;
}
