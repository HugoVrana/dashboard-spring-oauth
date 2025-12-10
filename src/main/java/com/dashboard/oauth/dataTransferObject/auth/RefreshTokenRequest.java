package com.dashboard.oauth.dataTransferObject.auth;

import lombok.Data;

@Data
public class RefreshTokenRequest{
    private String refreshToken;
}
