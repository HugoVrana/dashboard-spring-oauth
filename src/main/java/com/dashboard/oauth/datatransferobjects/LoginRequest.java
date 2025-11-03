package com.dashboard.oauth.datatransferobjects;

public record LoginRequest(
        String email,
        String password
) {}
