package com.dashboard.oauth.mapper.interfaces;

import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientRead;
import com.dashboard.oauth.model.entities.oauth.OAuthClient;

public interface IOAuthClientMapper {
    OAuthClientRead toRead(OAuthClient oAuthClient);
}
