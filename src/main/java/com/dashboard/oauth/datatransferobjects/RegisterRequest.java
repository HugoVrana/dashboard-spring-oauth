package com.dashboard.oauth.datatransferobjects;

public record RegisterRequest(
        String email,
        String password,
        String name
) {}
