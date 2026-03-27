package com.dashboard.oauth.mapper.interfaces;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.v2.TokenResponse;

public interface ITokenResponseMapper {
    TokenResponse toTokenResponse(AuthResponse authResponse);
}
