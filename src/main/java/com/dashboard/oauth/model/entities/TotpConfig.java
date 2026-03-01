package com.dashboard.oauth.model.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotpConfig extends BaseTwoFactorConfig {
    private String secret;
}
