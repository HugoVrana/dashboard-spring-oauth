package com.dashboard.oauth.model.entities.mfa;

import com.dashboard.common.model.Audit;
import lombok.Data;

@Data
public abstract class BaseTwoFactorConfig {
    private boolean enabled;
    private Audit audit;
}
