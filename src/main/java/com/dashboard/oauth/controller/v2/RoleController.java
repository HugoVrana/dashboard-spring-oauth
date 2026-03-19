package com.dashboard.oauth.controller.v2;

import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
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
@RestController("v2RoleController")
@RequiredArgsConstructor
@RequestMapping("api/v2/role")
@Tag(name = "Roles", description = "Role management")
@SecurityRequirement(name = "bearerAuth")
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
    @PostMapping("/")
    public ResponseEntity<RoleRead> createRole(@Valid @RequestBody CreateRole createRole) {
        return ResponseEntity.ok(roleService.createRole(createRole));
    }

    @Operation(summary = "Delete a role")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role deleted"),
            @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "Role ID", required = true) @PathVariable String id) {
        roleService.deleteRole(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }
}
