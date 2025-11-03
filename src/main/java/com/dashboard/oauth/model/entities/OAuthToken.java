package com.dashboard.oauth.model.entities;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

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
    private Date expiryDate;
    private Audit audit;
}
