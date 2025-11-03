package com.dashboard.oauth.model.entities;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "oauth_clients")
public class OAuthClient {
    private Audit audit;
}
