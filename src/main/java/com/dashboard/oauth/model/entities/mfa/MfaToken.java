package com.dashboard.oauth.model.entities.mfa;

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
@Document(collection = "mfa_tokens")
public class MfaToken {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String token;

    private ObjectId userId;
    private ObjectId authorizationRequestId;
    private boolean used;

    @Indexed(expireAfter = "0")
    private Instant expiryDate;

    private Audit audit;
}
