package com.dashboard.oauth.dataTransferObject.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class TokenIntrospectionResponse {
    @NotBlank(message = "Token is required")
    private Boolean isActive;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Grants are required")
    private List<String> grants;

    @NotBlank(message = "Expiration is required")
    private Long expiration;
}