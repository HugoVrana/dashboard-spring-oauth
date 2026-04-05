package com.dashboard.oauth.model.entities.user;

import com.dashboard.common.model.Audit;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class VerificationToken {
    @Id
    private ObjectId _id;

    @NotNull
    private Instant expiryDate;

    private boolean used;

    private Instant usedAt;

    private Instant emailSentAt;

    private Audit audit;

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
