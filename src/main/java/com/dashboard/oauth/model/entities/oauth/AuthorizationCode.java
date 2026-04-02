package com.dashboard.oauth.model.entities.oauth;

import com.dashboard.common.model.Audit;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    private String clientId;

    private ObjectId userId;

    @NotBlank
    private String redirectUri;

    @NotBlank
    private String codeChallenge;

    @NotBlank
    private String codeChallengeMethod;

    private String scope;

    private String state;

    private String nonce;

    private boolean used;

    @Indexed(expireAfter = "0")
    private Instant expiryDate;

    private Audit audit;
}
