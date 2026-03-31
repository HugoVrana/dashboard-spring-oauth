package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientRead;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;

public interface IOAuthClientService {
    OAuthClientRead getClient(ObjectId clientId);

    OAuthClientCreated createClient(OAuthClientCreate client);

    OAuthClientCreated createClient(OAuthClientCreate client, String rawSecret);

    void deleteClient(ObjectId clientId);

    OAuthClientCreated rotateSecret(ObjectId clientId);

    boolean validateClientCredentials(String authorizationHeader);

    boolean isRegisteredClient(String clientId);

    boolean isAllowedHost(String clientId, HttpServletRequest request);
}
