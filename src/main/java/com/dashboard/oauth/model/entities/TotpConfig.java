package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotpConfig extends BaseTwoFactorConfig {
    private String secret;
}
