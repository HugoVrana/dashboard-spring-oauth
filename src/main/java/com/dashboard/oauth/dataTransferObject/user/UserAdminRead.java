package com.dashboard.oauth.dataTransferObject.user;

import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserAdminRead {
    private String id;
    private String email;
    private Boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private Boolean locked;
    private Integer failedLoginAttempts;
    private String profileImageUrl;
    private List<RoleRead> roles;
    private Instant createdAt;
    private Instant deletedAt;
}
