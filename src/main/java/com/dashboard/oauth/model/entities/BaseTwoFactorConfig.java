package com.dashboard.oauth.model.entities;

import com.dashboard.common.model.Audit;
import lombok.Data;

@Data
public abstract class BaseTwoFactorConfig {
    private Boolean enabled = false;
    private Audit audit;
}
