package com.dashboard.oauth.controller.v2;

import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.role.RoleUpdate;
import com.dashboard.oauth.service.interfaces.IRoleService;
import jakarta.validation.Valid;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("api/v2/role")
@RestController("v2RoleController")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles", description = "Role management")
public class RoleController {
    private final IRoleService roleService;

    @Operation(summary = "Get all roles", description = "Returns all roles with their grants")
    @ApiResponse(responseCode = "200", description = "List of roles",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleRead.class))))
    @GetMapping("/")
    public ResponseEntity<List<RoleRead>> getAllRoles() {
        return ResponseEntity.ok(roleService.getRoles());
    }

    @Operation(summary = "Get role by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role found",
                    content = @Content(schema = @Schema(implementation = RoleRead.class))),
            @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<RoleRead> getRoleById(
            @Parameter(description = "Role ID", required = true) @PathVariable String id) {
        return ResponseEntity.ok(roleService.getRoleReadById(new ObjectId(id)));
    }

    @Operation(summary = "Create a role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role created",
                    content = @Content(schema = @Schema(implementation = RoleRead.class))),
            @ApiResponse(responseCode = "409", description = "Role with this name already exists", content = @Content)
    })
    @PreAuthorize("hasAuthority('dashboard-oauth-role-create')")
    @PostMapping("/")
    public ResponseEntity<RoleRead> createRole(@Valid @RequestBody CreateRole createRole) {
        return ResponseEntity.ok(roleService.createRole(createRole));
    }

    @Operation(summary = "Update a role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated",
                    content = @Content(schema = @Schema(implementation = RoleRead.class))),
            @ApiResponse(responseCode = "404", description = "Role not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Role with this name already exists", content = @Content)
    })
    @PreAuthorize("hasAuthority('dashboard-oauth-role-update')")
    @PutMapping("/{id}")
    public ResponseEntity<RoleRead> updateRole(
            @Parameter(description = "Role ID", required = true) @PathVariable String id,
            @Valid @RequestBody RoleUpdate update) {
        return ResponseEntity.ok(roleService.updateRole(new ObjectId(id), update));
    }

    @Operation(summary = "Add grant to role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grant added",
                    content = @Content(schema = @Schema(implementation = RoleRead.class))),
            @ApiResponse(responseCode = "404", description = "Role or grant not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Grant is already assigned to this role", content = @Content)
    })
    @PreAuthorize("hasAuthority('dashboard-oauth-role-manage-grants')")
    @PostMapping("/{id}/grants/{grantId}")
    public ResponseEntity<RoleRead> addGrantToRole(
            @Parameter(description = "Role ID", required = true) @PathVariable String id,
            @Parameter(description = "Grant ID", required = true) @PathVariable String grantId) {
        return ResponseEntity.ok(roleService.addGrantToRole(new ObjectId(id), new ObjectId(grantId)));
    }

    @Operation(summary = "Remove grant from role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grant removed",
                    content = @Content(schema = @Schema(implementation = RoleRead.class))),
            @ApiResponse(responseCode = "404", description = "Role not found or grant not assigned", content = @Content)
    })
    @PreAuthorize("hasAuthority('dashboard-oauth-role-manage-grants')")
    @DeleteMapping("/{id}/grants/{grantId}")
    public ResponseEntity<RoleRead> removeGrantFromRole(
            @Parameter(description = "Role ID", required = true) @PathVariable String id,
            @Parameter(description = "Grant ID", required = true) @PathVariable String grantId) {
        return ResponseEntity.ok(roleService.removeGrantFromRole(new ObjectId(id), new ObjectId(grantId)));
    }

    @Operation(summary = "Delete a role")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role deleted"),
            @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
    })
    @PreAuthorize("hasAuthority('dashboard-oauth-role-delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "Role ID", required = true) @PathVariable String id) {
        roleService.deleteRole(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }
}
