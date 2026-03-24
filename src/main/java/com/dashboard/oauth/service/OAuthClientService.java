package com.dashboard.oauth.service;

import com.dashboard.common.model.ActivityEvent;
import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.authentication.GrantsAuthentication;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientRead;
import com.dashboard.oauth.mapper.interfaces.IOAuthClientMapper;
import com.dashboard.oauth.model.entities.OAuthClient;
import com.dashboard.oauth.model.enums.ActivityEventType;
import com.dashboard.oauth.repository.IOauthClientRepository;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthClientService implements IOAuthClientService {

    private final IOauthClientRepository oauthClientRepository;
    private final IOAuthClientMapper oAuthClientMapper;
    private final PasswordEncoder passwordEncoder;
    private final IActivityFeedService activityFeedService;

    @Override
    public OAuthClientRead getClient(ObjectId clientId) {
        OAuthClient client = oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("OAuth client not found"));
        return oAuthClientMapper.toRead(client);
    }

    @Override
    public OAuthClientCreated createClient(OAuthClientCreate request) {
        String rawSecret = UUID.randomUUID().toString();

        Instant now = Instant.now();
        Audit audit = new Audit();
        audit.setCreatedAt(now);
        audit.setUpdatedAt(now);

        OAuthClient client = OAuthClient.builder()
                .clientSecret(passwordEncoder.encode(rawSecret))
                .redirectUris(request.getRedirectUris())
                .allowedScopes(request.getAllowedScopes())
                .audit(audit)
                .build();

        client = oauthClientRepository.save(client);

        publishActivityEvent(ActivityEventType.OAUTH_CLIENT_CREATED, client);

        OAuthClientCreated created = new OAuthClientCreated();
        created.setId(client.get_id().toHexString());
        created.setRedirectUris(client.getRedirectUris());
        created.setAllowedScopes(client.getAllowedScopes());
        created.setClientSecret(rawSecret);
        return created;
    }

    @Override
    public void deleteClient(ObjectId clientId) {
        OAuthClient client = oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("OAuth client not found"));

        client.getAudit().setDeletedAt(Instant.now());
        oauthClientRepository.save(client);

        publishActivityEvent(ActivityEventType.OAUTH_CLIENT_DELETED, client);
    }

    @Override
    public OAuthClientCreated rotateSecret(ObjectId clientId) {
        OAuthClient client = oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("OAuth client not found"));

        String rawSecret = UUID.randomUUID().toString();
        client.setClientSecret(passwordEncoder.encode(rawSecret));
        client.getAudit().setUpdatedAt(Instant.now());
        oauthClientRepository.save(client);

        OAuthClientCreated created = new OAuthClientCreated();
        created.setId(client.get_id().toHexString());
        created.setRedirectUris(client.getRedirectUris());
        created.setAllowedScopes(client.getAllowedScopes());
        created.setClientSecret(rawSecret);
        return created;
    }

    @Override
    public boolean validateClientCredentials(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(authorizationHeader.substring(6)), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return false;
            }
            String clientId = decoded.substring(0, colon);
            String clientSecret = decoded.substring(colon + 1);

            if (!org.bson.types.ObjectId.isValid(clientId)) {
                return false;
            }

            return oauthClientRepository
                    .findBy_idAndAudit_DeletedAtIsNull(new org.bson.types.ObjectId(clientId))
                    .filter(client -> client.getClientSecret() != null)
                    .map(client -> passwordEncoder.matches(clientSecret, client.getClientSecret()))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void publishActivityEvent(ActivityEventType type, OAuthClient client) {
        String actorId = null;
        String actorImageUrl = "";
        try {
            GrantsAuthentication auth = GrantsAuthentication.current();
            actorId = auth.getUserId();
            actorImageUrl = auth.getProfileImageUrlOrEmpty();
        } catch (IllegalStateException ignored) {
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("clientId", client.get_id().toHexString());
        metadata.put("userImageUrl", actorImageUrl);

        ActivityEvent event = ActivityEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .type(type.name())
                .actorId(actorId)
                .metadata(metadata)
                .build();
        activityFeedService.publishEvent(event);
    }
}
