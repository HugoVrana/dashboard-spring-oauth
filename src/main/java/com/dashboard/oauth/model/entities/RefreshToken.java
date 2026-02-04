package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

@Document(collection = "refresh_tokens")
@Data
@Builder
public class RefreshToken {
    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private ObjectId token;

    @Field("user_id")
    private String userId;

    @Indexed(expireAfter = "0")
    private Instant expiryDate;

    private Audit audit;
}