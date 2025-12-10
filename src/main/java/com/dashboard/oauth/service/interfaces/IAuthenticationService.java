package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.*;

public interface IAuthenticationService {
    UserInfo register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse refreshToken(String refreshToken);
    void logout(String userId);
}
