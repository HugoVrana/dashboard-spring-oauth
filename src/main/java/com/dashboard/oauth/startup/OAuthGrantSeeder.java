package com.dashboard.oauth.startup;

import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.repository.IOauthClientRepository;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthGrantSeeder implements ApplicationRunner {

    private final IGrantService grantService;
    private final IOAuthClientService clientService;
    private final IOauthClientRepository clientRepository;

    private static final List<Map.Entry<String, String>> REQUIRED_GRANTS = List.of(
            Map.entry("dashboard-oauth-grant-create",              "Create OAuth grants"),
            Map.entry("dashboard-oauth-grant-delete",              "Delete OAuth grants"),
            Map.entry("dashboard-oauth-role-create",               "Create OAuth roles"),
            Map.entry("dashboard-oauth-role-delete",               "Delete OAuth roles"),
            Map.entry("dashboard-oauth-user-update",               "Update users"),
            Map.entry("dashboard-oauth-user-delete",               "Delete users"),
            Map.entry("dashboard-oauth-user-block",                "Block and unblock users"),
            Map.entry("dashboard-oauth-user-resend-verification",  "Resend verification emails to users"),
            Map.entry("dashboard-oauth-user-reset-password",       "Trigger password resets for users"),
            Map.entry("dashboard-oauth-client-create",             "Register OAuth clients"),
            Map.entry("dashboard-oauth-client-delete",             "Delete OAuth clients"),
            Map.entry("dashboard-oauth-client-rotate-secret",      "Rotate OAuth client secrets")
    );

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking required OAuth grants...");
        int grantsCreated = 0;

        for (Map.Entry<String, String> entry : REQUIRED_GRANTS) {
            String name = entry.getKey();
            if (grantService.getGrantByName(name).isEmpty()) {
                GrantCreate grantCreate = new GrantCreate();
                grantCreate.setName(name);
                grantCreate.setDescription(entry.getValue());
                grantService.createGrant(grantCreate);
                log.info("Created missing grant: {}", name);
                grantsCreated++;
            }
        }

        if (grantsCreated == 0) {
            log.info("All required OAuth grants are present.");
        } else {
            log.info("Seeded {} missing OAuth grant(s).", grantsCreated);
        }

        final List<String> frontendRedirectUris = List.of(
                "http://localhost:3000/api/auth/callback",
                "http://localhost:3000/api/auth/callback/dashboard-oauth"
        );
        boolean allUrlsExists = true;
        for (String frontendRedirectUri : frontendRedirectUris) {
            boolean exists = clientRepository.existsByRedirectUrisContainingAndAudit_DeletedAtIsNull(frontendRedirectUri);
            allUrlsExists = allUrlsExists && exists;
        }
        if (!allUrlsExists) {
            OAuthClientCreate oauthClientCreate = new OAuthClientCreate();
            oauthClientCreate.setRedirectUris(frontendRedirectUris);
            oauthClientCreate.setAllowedScopes(List.of("openid", "profile", "email"));
            oauthClientCreate.setAllowedHosts(List.of("http://localhost:3000"));
            OAuthClientCreated clientCreated = clientService.createClient(oauthClientCreate, "dev-secret");
            log.info("Seeded frontend OAuth client. ID: {}, Secret: {}", clientCreated.getId(), clientCreated.getClientSecret());
        } else {
            log.info("Frontend OAuth client already exists, skipping.");
        }
    }
}
