package com.dashboard.oauth.dataTransferObject.auth;

import lombok.Data;

@Data
public class LoginRequest{
    private String email;
    private String password;
}
