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
@Document(collection = "authorization_codes")
public class AuthorizationCode {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String code;

    private String clientId;
    private String userId;
    private String redirectUri;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String scope;
    private String state;
    private String nonce;
    private boolean used;

    @Indexed(expireAfter = "0")
    private Instant expiryDate;

    private Audit audit;
}
