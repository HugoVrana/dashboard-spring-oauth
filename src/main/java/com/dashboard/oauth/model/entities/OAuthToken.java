package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "oauth_tokens")
public class OAuthToken {
    @Id
    private ObjectId _id;

    @Indexed(unique = true)
    private String token;

    @DBRef
    private User user;

    @Indexed(expireAfter = "0")
    private Instant expiryDate;

    private Audit audit;
}
