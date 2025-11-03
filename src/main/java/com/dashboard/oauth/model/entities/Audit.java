package com.dashboard.oauth.model.entities;

import lombok.Data;
import java.time.Instant;

@Data
public class Audit {
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}