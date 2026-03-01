package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.totp.TotpSetupResponse;

public interface ITotpService {
    TotpSetupResponse setupTotp(String userId);

    boolean verifyTotp(String userId, String code);
}
