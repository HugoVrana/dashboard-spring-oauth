package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.datatransferobjects.AuthResponse;
import com.dashboard.oauth.datatransferobjects.LoginRequest;
import com.dashboard.oauth.datatransferobjects.RegisterRequest;

public interface IAuthenticationService {
    AuthResponse register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse refreshToken(String refreshToken);
    void logout(String userId);
}
