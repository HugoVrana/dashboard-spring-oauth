package com.dashboard.oauth.dataTransferObject.auth;

import com.dashboard.oauth.model.UserInfo;
import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;
}