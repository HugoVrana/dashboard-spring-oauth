package com.dashboard.oauth.dataTransferObject.auth;

import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private UserInfoRead user;
    private String accessToken;
    private boolean requiresTwoFactorEnrollment;
    private String nextStep;
}
