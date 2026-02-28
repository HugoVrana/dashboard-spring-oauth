package com.dashboard.oauth.dataTransferObject.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {
    private boolean valid;
    private String reason;

    public static TokenValidationResponse valid() {
        return new TokenValidationResponse(true, null);
    }

    public static TokenValidationResponse invalid(String reason) {
        return new TokenValidationResponse(false, reason);
    }
}
