package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Data
@Document(collection = "users")
public class User {
    @Id
    private ObjectId _id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private Boolean emailVerified = false;

    private LocalDateTime emailVerifiedAt;

    private Optional<VerificationToken> emailVerificationToken;

    private Optional<VerificationToken> passwordResetToken;

    @DBRef
    private List<Role> roles;
    
    private Audit audit;
}
