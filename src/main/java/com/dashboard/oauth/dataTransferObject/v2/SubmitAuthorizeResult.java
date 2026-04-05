package com.dashboard.oauth.dataTransferObject.v2;

import com.dashboard.oauth.model.entities.oauth.AuthorizationCode;

public record SubmitAuthorizeResult(boolean mfaRequired, String mfaToken, AuthorizationCode authorizationCode) {}
