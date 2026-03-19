package com.dashboard.oauth.controller.v2;

import com.dashboard.oauth.dataTransferObject.user.UserAdminRead;
import com.dashboard.oauth.dataTransferObject.user.UserAdminUpdate;
import com.dashboard.oauth.service.interfaces.IUserService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController("v2UserController")
@RequiredArgsConstructor
@RequestMapping("api/v2/user")
@Tag(name = "Users (Admin)", description = "Admin user management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final IUserService userService;

    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "List of all users",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserAdminRead.class))))
    @GetMapping("/")
    public ResponseEntity<List<UserAdminRead>> getAllUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    @Operation(summary = "Search users by email", description = "Case-insensitive partial match on email address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching users",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserAdminRead.class))))
    })
    @GetMapping("/search")
    public ResponseEntity<List<UserAdminRead>> searchUsers(
            @Parameter(description = "Search query (matched against email)", required = true) @RequestParam String q) {
        return ResponseEntity.ok(userService.searchUsers(q));
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserAdminRead.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserAdminRead> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        return ResponseEntity.ok(userService.getUserAdminReadById(new ObjectId(id)));
    }

    @Operation(summary = "Update user", description = "Update mutable user fields (e.g. email)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated",
                    content = @Content(schema = @Schema(implementation = UserAdminRead.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserAdminRead> updateUser(
            @Parameter(description = "User ID", required = true) @PathVariable String id,
            @Valid @RequestBody UserAdminUpdate update) {
        return ResponseEntity.ok(userService.updateUser(new ObjectId(id), update));
    }

    @Operation(summary = "Delete user", description = "Soft-deletes the user account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        userService.deleteUser(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Block user", description = "Locks the user account, preventing login")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User blocked"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "User is already blocked", content = @Content)
    })
    @PostMapping("/{id}/block")
    public ResponseEntity<Void> blockUser(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        userService.blockUser(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unblock user", description = "Unlocks the user account and resets failed login attempts")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User unblocked"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "User is not blocked", content = @Content)
    })
    @PostMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockUser(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        userService.unblockUser(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resend verification email",
            description = "Creates a new email verification token; the scheduler will send it on its next run")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Verification email queued"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email is already verified", content = @Content)
    })
    @PostMapping("/{id}/resend-verification")
    public ResponseEntity<Void> resendVerificationEmail(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        userService.resendVerificationEmail(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Trigger password reset",
            description = "Creates a new password reset token; the scheduler will send the email on its next run")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset email queued"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> triggerPasswordReset(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        userService.triggerPasswordReset(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }
}
