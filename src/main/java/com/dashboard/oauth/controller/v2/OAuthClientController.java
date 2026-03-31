package com.dashboard.oauth.controller.v2;

import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientRead;
import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("v2/oauthclients")
// @SecurityRequirement(name = "bearerAuth")
@RestController("v2OAuthClientController")
@Tag(name = "OAuth Clients", description = "OAuth Client Management")
public class OAuthClientController {
    private final IOAuthClientService oAuthClientService;

    @Operation(summary = "Get OAuth client by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client found",
                    content = @Content(schema = @Schema(implementation = OAuthClientRead.class))),
            @ApiResponse(responseCode = "404", description = "Client not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<OAuthClientRead> getClient(@PathVariable String id) {
        return ResponseEntity.ok(oAuthClientService.getClient(new ObjectId(id)));
    }

    @Operation(summary = "Register a new OAuth client",
            description = "Creates a client and returns the client secret. The secret is shown only once.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client created",
                    content = @Content(schema = @Schema(implementation = OAuthClientCreated.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAuthority('dashboard-oauth-client-create')")
    @PostMapping("/")
    public ResponseEntity<OAuthClientCreated> createClient(@Valid @RequestBody OAuthClientCreate request) {
        return ResponseEntity.ok(oAuthClientService.createClient(request));
    }

    @Operation(summary = "Delete an OAuth client")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client deleted"),
            @ApiResponse(responseCode = "404", description = "Client not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAuthority('dashboard-oauth-client-delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable String id) {
        oAuthClientService.deleteClient(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rotate client secret",
            description = "Generates a new client secret. The old secret is immediately invalidated. The new secret is shown only once.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New secret issued",
                    content = @Content(schema = @Schema(implementation = OAuthClientCreated.class))),
            @ApiResponse(responseCode = "404", description = "Client not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
   // @PreAuthorize("hasAuthority('dashboard-oauth-client-rotate-secret')")
    @PostMapping("/{id}/secret")
    public ResponseEntity<OAuthClientCreated> rotateSecret(@PathVariable String id) {
        return ResponseEntity.ok(oAuthClientService.rotateSecret(new ObjectId(id)));
    }
}
