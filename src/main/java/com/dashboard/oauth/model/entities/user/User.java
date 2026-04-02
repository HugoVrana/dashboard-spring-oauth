package com.dashboard.oauth.model.entities.user;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.entities.mfa.BaseTwoFactorConfig;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private ObjectId _id;

    @NotBlank
    @Indexed(unique = true)
    private String email;

    @NotBlank
    private String password;

    private boolean emailVerified;

    private LocalDateTime emailVerifiedAt;

    private VerificationToken emailVerificationToken;

    private VerificationToken passwordResetToken;

    private ObjectId profileImageId;

    @Min(0)
    @NotNull
    private Integer failedLoginAttempts;

    private boolean locked;

    @DBRef
    private List<Role> roles;

    private BaseTwoFactorConfig twoFactorConfig;

    private Audit audit;
}
