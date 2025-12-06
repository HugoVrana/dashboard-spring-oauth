package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Date;

@Document(collection = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    private ObjectId id;
    @Indexed(unique = true)
    private String token;
    @Field("user_id")
    private String userId;
    @Indexed(expireAfter = "0")
    private Date expiryDate;
    private Audit audit;

    public RefreshToken(String tokenValue, String hexString, Instant instant) {
        this.token = tokenValue;
        this.userId = hexString;
        this.expiryDate = Date.from(instant);
    }
}