package com.dashboard.oauth.mapper;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.v2.TokenResponse;
import com.dashboard.oauth.mapper.interfaces.ITokenResponseMapper;
import org.springframework.stereotype.Service;

@Service
public class TokenResponseMapper implements ITokenResponseMapper {
    @Override
    public TokenResponse toTokenResponse(AuthResponse authResponse) {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(authResponse.getAccessToken());
        tokenResponse.setExpiresIn(authResponse.getExpiresIn() / 1000);
        tokenResponse.setRefreshToken(authResponse.getRefreshToken());
        tokenResponse.setIdToken(authResponse.getIdToken());
        tokenResponse.setScope(authResponse.getScope());
        return tokenResponse;
    }
}
