package com.dashboard.oauth.service;

import com.dashboard.common.model.ActivityEvent;
import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.authentication.GrantsAuthentication;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientRead;
import jakarta.servlet.http.HttpServletRequest;
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        return createClient(request, UUID.randomUUID().toString());
    }

    @Override
    public OAuthClientCreated createClient(OAuthClientCreate request, String rawSecret) {

        Instant now = Instant.now();
        Audit audit = new Audit();
        audit.setCreatedAt(now);
        audit.setUpdatedAt(now);

        List<String> allowedHosts = normalizeAllowedHosts(request.getAllowedHosts());
        List<String> redirectUris = normalizeRedirectUris(request.getRedirectUris());
        List<String> redirectHosts = redirectUris.stream()
                .map(this::extractOrigin)
                .distinct()
                .toList();

        if (!allowedHosts.containsAll(redirectHosts)) {
            throw new InvalidRequestException("Every redirect URI host must be included in allowedHosts");
        }

        OAuthClient client = OAuthClient.builder()
                .clientSecret(passwordEncoder.encode(rawSecret))
                .redirectUris(redirectUris)
                .allowedHosts(allowedHosts)
                .allowedScopes(request.getAllowedScopes())
                .audit(audit)
                .build();

        client = oauthClientRepository.save(client);

        publishActivityEvent(ActivityEventType.OAUTH_CLIENT_CREATED, client);

        OAuthClientCreated created = new OAuthClientCreated();
        created.setId(client.get_id().toHexString());
        created.setRedirectUris(client.getRedirectUris());
        created.setAllowedHosts(client.getAllowedHosts());
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
        created.setAllowedHosts(client.getAllowedHosts());
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

    @Override
    public boolean isRegisteredClient(String clientId) {
        return getActiveClient(clientId).isPresent();
    }

    @Override
    public boolean isAllowedHost(String clientId, HttpServletRequest request) {
        Optional<OAuthClient> optionalOAuthClient = getActiveClient(clientId);
        if (optionalOAuthClient.isEmpty()) {
            return false;
        }
        OAuthClient client = optionalOAuthClient.get();

        if (client.getAllowedHosts() == null || client.getAllowedHosts().isEmpty()) {
            return false;
        }

        String callerHost = extractCallerHost(request);
        if (callerHost == null) {
            return false;
        }


        return  client.getAllowedHosts().contains(callerHost);
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

    private Optional<OAuthClient> getActiveClient(String clientId) {
        if (clientId == null || !ObjectId.isValid(clientId)) {
            return Optional.empty();
        }

        return oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(new ObjectId(clientId));
    }

    private List<String> normalizeAllowedHosts(List<String> allowedHosts) {
        return allowedHosts.stream()
                .map(this::normalizeOrigin)
                .distinct()
                .toList();
    }

    private List<String> normalizeRedirectUris(List<String> redirectUris) {
        return redirectUris.stream()
                .map(this::normalizeAbsoluteUri)
                .distinct()
                .toList();
    }

    private String extractCallerHost(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return normalizeOrigin(origin);
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return normalizeOrigin(referer);
        }

        return null;
    }

    private String extractOrigin(String uriValue) {
        return normalizeOrigin(uriValue);
    }

    private String normalizeAbsoluteUri(String uriValue) {
        URI uri = parseUri(uriValue, "redirectUri");
        if (uri.getPath() == null || uri.getPath().isBlank()) {
            throw new InvalidRequestException("redirectUri must include a path");
        }
        return uri.toString();
    }

    private String normalizeOrigin(String uriValue) {
        URI uri = parseUri(uriValue, "allowedHost");
        return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
    }

    private URI parseUri(String uriValue, String fieldName) {
        try {
            URI uri = URI.create(uriValue.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new InvalidRequestException(fieldName + " must be an absolute http/https URL");
            }
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new InvalidRequestException(fieldName + " must use http or https");
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid " + fieldName + ": " + uriValue);
        }
    }
}
