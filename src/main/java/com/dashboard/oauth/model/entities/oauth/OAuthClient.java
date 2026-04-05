package com.dashboard.oauth.model.entities.oauth;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.entities.auth.Grant;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@Document(collection = "oauth_clients")
public class OAuthClient {

    @Id
    private ObjectId _id;

    private String clientSecret;

    private List<String> redirectUris;

    private List<String> allowedHosts;

    private List<String> allowedScopes;

    @DBRef
    private List<Grant> allowedGrants;

    private Audit audit;
}
