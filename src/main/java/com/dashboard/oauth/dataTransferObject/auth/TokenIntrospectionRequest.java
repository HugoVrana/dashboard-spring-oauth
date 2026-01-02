package com.dashboard.oauth.dataTransferObject.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenIntrospectionRequest {
    @NotBlank(message = "Token is required")
    public String token;
}
