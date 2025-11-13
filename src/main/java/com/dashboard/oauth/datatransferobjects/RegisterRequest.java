package com.dashboard.oauth.datatransferobjects;

import jakarta.validation.constraints.Email;

public record RegisterRequest(
        @Email
        String email,
        String password,
        String name
) {}
