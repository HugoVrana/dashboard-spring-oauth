package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientRead;
import org.bson.types.ObjectId;

public interface IOAuthClientService {
    OAuthClientRead getClient(ObjectId clientId);

    OAuthClientCreated createClient(OAuthClientCreate client);

    void deleteClient(ObjectId clientId);

    OAuthClientCreated rotateSecret(ObjectId clientId);

    boolean validateClientCredentials(String authorizationHeader);
}
