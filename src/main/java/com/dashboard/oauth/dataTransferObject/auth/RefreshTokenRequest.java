package com.dashboard.oauth.dataTransferObject.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Token is required")
    private String refreshToken;
}
