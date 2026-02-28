package com.dashboard.oauth.dataTransferObject.user;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserSelfUpdate {
    @Email
    private String email;

    private String password;
}
