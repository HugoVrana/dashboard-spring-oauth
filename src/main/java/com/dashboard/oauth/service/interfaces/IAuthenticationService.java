package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.role.AddRoleRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import org.springframework.security.core.Authentication;

public interface IAuthenticationService {
    UserInfoRead register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    AuthResponse refreshToken(String refreshToken);

    void logout(String authHeader);

    /**
     * RFC 7009 token revocation. Accepts either a refresh token (hex ObjectId)
     * or an access token (JWT). Silently succeeds if the token is not found.
     */
    void revokeToken(String token);

    AuthResponse addUserRole(AddRoleRequest request);

    void verifyEmail(String token);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);

    boolean validatePasswordResetToken(String token);

    UserInfoRead getCurrentUser(Authentication authentication);
}
