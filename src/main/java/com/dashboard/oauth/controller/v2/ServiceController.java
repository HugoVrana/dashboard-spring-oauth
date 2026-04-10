package com.dashboard.oauth.controller.v2;

import com.dashboard.oauth.dataTransferObject.grant.EnsureGrantsResponse;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/service")
@Tag(name = "Service", description = "Machine-to-machine endpoints for trusted services")
public class ServiceController {

    private final IGrantService grantService;
    private final IOAuthClientService oAuthClientService;

    @Operation(
            summary = "Ensure grants exist",
            description = "Idempotently creates any grants from the provided list that do not already exist. " +
                    "Requires HTTP Basic authentication using the service secret. " +
                    "Intended to be called by downstream services on startup."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grants ensured",
                    content = @Content(schema = @Schema(implementation = EnsureGrantsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service secret", content = @Content)
    })
    @PostMapping("/grants/ensure")
    public ResponseEntity<?> ensureGrants(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody List<GrantCreate> grants) {

        if (!oAuthClientService.validateClientCredentials(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"service\"")
                    .build();
        }

        return ResponseEntity.ok(grantService.ensureGrants(grants));
    }
}
