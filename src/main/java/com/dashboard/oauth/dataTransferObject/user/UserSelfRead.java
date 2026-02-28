package com.dashboard.oauth.dataTransferObject.user;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserSelfRead {
    private String id;
    private String email;
    private Boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private String profileImageUrl;
    private Boolean locked;
}