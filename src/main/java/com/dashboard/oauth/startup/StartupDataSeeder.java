package com.dashboard.oauth.startup;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.repository.IOauthClientRepository;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import com.dashboard.oauth.service.interfaces.IRoleService;
import com.dashboard.oauth.service.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDataSeeder implements ApplicationRunner {

    private static final String DEFAULT_SEED_PASSWORD = "Admin@seed1!";

    private final IRoleService roleService;
    private final IGrantService grantService;
    private final IOAuthClientService clientService;
    private final IOauthClientRepository clientRepository;
    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.admin.email:admin@localhost}")
    private String seedAdminEmail;

    @Value("${seed.admin.password:" + DEFAULT_SEED_PASSWORD + "}")
    private String seedAdminPassword;

    private static final List<Map.Entry<String, String>> REQUIRED_GRANTS = List.of(
            Map.entry("dashboard-oauth-grant-create",             "Create OAuth grants"),
            Map.entry("dashboard-oauth-grant-update",             "Update OAuth grants"),
            Map.entry("dashboard-oauth-grant-delete",             "Delete OAuth grants"),
            Map.entry("dashboard-oauth-role-create",              "Create OAuth roles"),
            Map.entry("dashboard-oauth-role-update",              "Update OAuth roles"),
            Map.entry("dashboard-oauth-role-delete",              "Delete OAuth roles"),
            Map.entry("dashboard-oauth-role-manage-grants",       "Add and remove grants on roles"),
            Map.entry("dashboard-oauth-user-update",              "Update users"),
            Map.entry("dashboard-oauth-user-delete",              "Delete users"),
            Map.entry("dashboard-oauth-user-block",               "Block and unblock users"),
            Map.entry("dashboard-oauth-user-resend-verification", "Resend verification emails to users"),
            Map.entry("dashboard-oauth-user-reset-password",      "Trigger password resets for users"),
            Map.entry("dashboard-oauth-client-create",            "Register OAuth clients"),
            Map.entry("dashboard-oauth-client-delete",            "Delete OAuth clients"),
            Map.entry("dashboard-oauth-client-rotate-secret",     "Rotate OAuth client secrets")
    );

    private static final Map<String, List<String>> REQUIRED_ROLES = Map.of(
            "role-manager",  List.of(
                    "dashboard-oauth-role-create",
                    "dashboard-oauth-role-update",
                    "dashboard-oauth-role-delete",
                    "dashboard-oauth-role-manage-grants"
            ),
            "grant-manager", List.of(
                    "dashboard-oauth-grant-create",
                    "dashboard-oauth-grant-update",
                    "dashboard-oauth-grant-delete"
            )
    );

    private static final List<String> REQUIRED_OAUTH_CLIENT_URIS = List.of(
            "http://localhost:3000/api/auth/callback",
            "http://localhost:3000/api/auth/callback/dashboard-oauth"
    );

    @Override
    public void run(@NonNull ApplicationArguments args) {
        seedGrants();
        seedRoles();
        seedAdminUser();
        seedOAuthClients();
    }

    private void seedGrants() {
        log.info("Checking required OAuth grants...");
        int created = 0;
        for (Map.Entry<String, String> entry : REQUIRED_GRANTS) {
            if (grantService.getGrantByName(entry.getKey()).isEmpty()) {
                GrantCreate dto = new GrantCreate();
                dto.setName(entry.getKey());
                dto.setDescription(entry.getValue());
                grantService.createGrant(dto);
                log.info("Created grant: {}", entry.getKey());
                created++;
            }
        }
        log.info(created > 0 ? "Seeded {} grant(s)." : "All required grants present.", created);
    }

    private void seedRoles() {
        log.info("Checking required OAuth roles...");
        for (Map.Entry<String, List<String>> entry : REQUIRED_ROLES.entrySet()) {
            String roleName = entry.getKey();
            List<String> grantNames = entry.getValue();

            Role role = roleService.getRoleByName(roleName).orElseGet(() -> {
                CreateRole dto = new CreateRole();
                dto.setName(roleName);
                roleService.createRole(dto);
                log.info("Created role: {}", roleName);
                return roleService.getRoleByName(roleName).orElseThrow();
            });

            for (String grantName : grantNames) {
                grantService.getGrantByName(grantName).ifPresent(grant -> {
                    boolean alreadyAssigned = role.getGrants().stream()
                            .anyMatch(g -> g.get_id().equals(grant.get_id()));
                    if (!alreadyAssigned) {
                        roleService.addGrantToRole(role.get_id(), grant.get_id());
                        log.info("Assigned grant '{}' to role '{}'", grantName, roleName);
                    }
                });
            }
        }
    }

    private void seedAdminUser() {
        if (userService.getUserByEmail(seedAdminEmail).isPresent()) {
            log.info("Seed admin user '{}' already exists, skipping.", seedAdminEmail);
            return;
        }

        List<Role> roles = new ArrayList<>();
        roleService.getRoleByName("role-manager").ifPresent(roles::add);
        roleService.getRoleByName("grant-manager").ifPresent(roles::add);

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        audit.setUpdatedAt(Instant.now());

        User user = new User();
        user.setEmail(seedAdminEmail);
        user.setPassword(passwordEncoder.encode(seedAdminPassword));
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        user.setRoles(roles);
        user.setAudit(audit);

        userService.saveUser(user);

        if (DEFAULT_SEED_PASSWORD.equals(seedAdminPassword)) {
            log.warn("Seed admin user created with default password. Set 'seed.admin.password' for non-dev environments.");
        }
        log.info("Seeded admin user: {}", seedAdminEmail);
    }

    private void seedOAuthClients() {
        boolean allExist = REQUIRED_OAUTH_CLIENT_URIS.stream()
                .allMatch(clientRepository::existsByRedirectUrisContainingAndAudit_DeletedAtIsNull);

        if (!allExist) {
            OAuthClientCreate dto = new OAuthClientCreate();
            dto.setRedirectUris(REQUIRED_OAUTH_CLIENT_URIS);
            dto.setAllowedScopes(List.of("openid", "profile", "email"));
            dto.setAllowedHosts(List.of("http://localhost:3000"));
            dto.setEmailVerificationRedirectPath("auth/verify-email");
            dto.setPasswordResetRedirectPath("auth/reset-password");
            OAuthClientCreated created = clientService.createClient(dto, "dev-secret");
            log.info("Seeded frontend OAuth client. ID: {}, Secret: {}", created.getId(), created.getClientSecret());
        } else {
            log.info("Frontend OAuth client already exists, skipping.");
        }
    }
}
