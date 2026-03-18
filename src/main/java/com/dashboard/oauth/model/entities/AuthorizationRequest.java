package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "authorization_requests")
public class AuthorizationRequest {

    @Id
    private ObjectId id;

    private String clientId;
    private String redirectUri;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String scope;
    private String state;
    private boolean used;

    @Indexed(expireAfter = "0")
    private Instant expiryDate;

    private Audit audit;
}
