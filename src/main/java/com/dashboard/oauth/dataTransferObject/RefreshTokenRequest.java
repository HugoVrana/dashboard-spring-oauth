package com.dashboard.oauth.dataTransferObject;

import lombok.Data;

@Data
public class RefreshTokenRequest{
    private String refreshToken;
}
