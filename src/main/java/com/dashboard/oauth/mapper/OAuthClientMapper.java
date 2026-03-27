package com.dashboard.oauth.mapper;

import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientRead;
import com.dashboard.oauth.mapper.interfaces.IOAuthClientMapper;
import com.dashboard.oauth.model.entities.OAuthClient;
import org.springframework.stereotype.Service;

@Service
public final class OAuthClientMapper implements IOAuthClientMapper {
    @Override
    public OAuthClientRead toRead(final OAuthClient oAuthClient) {
        OAuthClientRead read = new OAuthClientRead();
        read.setId(oAuthClient.get_id().toHexString());
        read.setRedirectUris(oAuthClient.getRedirectUris());
        read.setAllowedHosts(oAuthClient.getAllowedHosts());
        read.setAllowedScopes(oAuthClient.getAllowedScopes());
        return read;
    }
}
