package com.dashboard.oauth.model.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {
    private String token;
    private Instant expiryDate;
    private Boolean used = false;
    private Instant usedAt;
    private Instant createdAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}