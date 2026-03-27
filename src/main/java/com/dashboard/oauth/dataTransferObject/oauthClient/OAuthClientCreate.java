package com.dashboard.oauth.dataTransferObject.oauthClient;

import com.dashboard.oauth.validation.ValidRedirectUri;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class OAuthClientCreate {
    @NotEmpty
    private List<@ValidRedirectUri String> redirectUris;
    @NotEmpty
    private List<String> allowedScopes;
    @NotEmpty
    private List<String> allowedHosts;
}