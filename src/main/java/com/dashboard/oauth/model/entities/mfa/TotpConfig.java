package com.dashboard.oauth.model.entities.mfa;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotpConfig extends BaseTwoFactorConfig {
    @NotBlank
    private String secret;
}
