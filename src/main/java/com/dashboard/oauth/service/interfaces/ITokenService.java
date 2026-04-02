package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.model.entities.user.User;

public interface ITokenService {
    String createEmailVerificationToken(User user);

    String createPasswordResetToken(User user);

    void verifyEmail(String tokenValue);

    void resetPassword(String tokenValue, String newPassword);
}
