package com.dashboard.oauth.dataTransferObject.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MfaRequiredResponse {

    @JsonProperty("mfa_required")
    private boolean mfaRequired;

    @JsonProperty("mfa_token")
    private String mfaToken;
}
