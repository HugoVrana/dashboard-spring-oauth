package com.dashboard.oauth.model.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {
    @Id
    private ObjectId _id;
    private Instant expiryDate;
    private Boolean used = false;
    private Instant usedAt;
    private Instant emailSentAt;
    private Instant createdAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}