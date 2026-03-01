package com.dashboard.oauth.dataTransferObject.totp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotpSetupResponse {
    private String qrCodeDataUri;
    private String secret;
}
