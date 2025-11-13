package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.datatransferobjects.AuthResponse;
import com.dashboard.oauth.datatransferobjects.LoginRequest;
import com.dashboard.oauth.datatransferobjects.RegisterRequest;
import com.dashboard.oauth.datatransferobjects.UserInfo;

public interface IAuthenticationService {
    UserInfo register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    AuthResponse refreshToken(String refreshToken);
    void logout(String userId);
}
