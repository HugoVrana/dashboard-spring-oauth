package com.dashboard.oauth.dataTransferObject.auth;

import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String idToken;
    private String tokenType;
    private Long expiresIn;
    private String scope;
    private UserInfoRead user;
}