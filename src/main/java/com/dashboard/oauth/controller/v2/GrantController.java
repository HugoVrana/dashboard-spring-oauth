package com.dashboard.oauth.controller.v2;

import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.service.GrantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController("v2GrantController")
@RequiredArgsConstructor
@RequestMapping("api/v2/grant")
@Tag(name = "Grants", description = "Grant (permission) management")
@SecurityRequirement(name = "bearerAuth")
public class GrantController {
    private final GrantService grantService;

    @Operation(summary = "Get all grants", description = "Returns all grants")
    @ApiResponse(responseCode = "200", description = "List of grants",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = GrantRead.class))))
    @GetMapping("/")
    public ResponseEntity<List<GrantRead>> getAllGrants() {
        return ResponseEntity.ok(grantService.getGrants());
    }

    @Operation(summary = "Get grant by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grant found",
                    content = @Content(schema = @Schema(implementation = GrantRead.class))),
            @ApiResponse(responseCode = "404", description = "Grant not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<GrantRead> getGrantById(
            @Parameter(description = "Grant ID", required = true) @PathVariable String id) {
        return ResponseEntity.ok(grantService.getGrantReadById(new ObjectId(id)));
    }

    @Operation(summary = "Create a grant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grant created",
                    content = @Content(schema = @Schema(implementation = GrantRead.class))),
            @ApiResponse(responseCode = "409", description = "Grant with this name already exists", content = @Content)
    })
    @PostMapping("/")
    public ResponseEntity<GrantRead> createGrant(@RequestBody GrantCreate grantCreate) {
        return ResponseEntity.ok(grantService.createGrant(grantCreate));
    }

    @Operation(summary = "Delete a grant")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Grant deleted"),
            @ApiResponse(responseCode = "404", description = "Grant not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrant(
            @Parameter(description = "Grant ID", required = true) @PathVariable String id) {
        grantService.deleteGrant(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }
}
