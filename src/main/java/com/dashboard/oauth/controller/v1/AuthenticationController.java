package com.dashboard.oauth.controller.v1;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.ForgotPasswordRequest;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RefreshTokenRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.auth.ResetPasswordRequest;
import com.dashboard.oauth.dataTransferObject.auth.TokenValidationResponse;
import com.dashboard.oauth.dataTransferObject.role.AddRoleRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@Deprecated(since = "April 5th 2026", forRemoval = true)
@RequestMapping(value = "/api/v1/auth", produces = "application/json")
@Tag(name = "Authentication", description = "Authentication operations")
public class AuthenticationController {

    private final IAuthenticationService authService;


    @Operation(summary = "Register a new user", description = "Creates a new user account with email, password, and role")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = UserInfoRead.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<UserInfoRead> register(@Valid @RequestBody RegisterRequest request) {
        UserInfoRead response = authService.register(request);
        URI location = URI.create("/api/auth/register");
        return ResponseEntity.created(location).body(response);
    }


    @Operation(summary = "Login", description = "Authenticates user and returns access and refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "423", description = "Account is locked")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }


    @Operation(summary = "Refresh token", description = "Exchanges a refresh token for a new access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }


    @Operation(summary = "Logout", description = "Invalidates the user's refresh token")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Verify email", description = "Verifies user's email address using the verification token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @Parameter(description = "Email verification token", required = true)
            @RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Forgot password", description = "Initiates password reset by sending a reset email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset email sent if account exists")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Reset password", description = "Resets user's password using the reset token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Validate reset token", description = "Checks if a password reset token is valid")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid",
                    content = @Content(schema = @Schema(implementation = TokenValidationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token is invalid or expired",
                    content = @Content(schema = @Schema(implementation = TokenValidationResponse.class)))
    })
    @GetMapping("/validate-reset-token")
    public ResponseEntity<TokenValidationResponse> validateResetToken(
            @Parameter(description = "Password reset token", required = true)
            @RequestParam String token) {
        boolean isValid = authService.validatePasswordResetToken(token);
        if (isValid) {
            return ResponseEntity.ok(TokenValidationResponse.valid());
        }
        return ResponseEntity.badRequest().body(TokenValidationResponse.invalid("Token is invalid or expired"));
    }
    
    @Operation(summary = "Add role to user", description = "Assigns a role to a user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role added successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid user or role ID"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "409", description = "User already has this role")
    })
    @PostMapping("/user/role")
    public ResponseEntity<AuthResponse> addUserRole(@Valid @RequestBody AddRoleRequest request) {
        return ResponseEntity.ok(authService.addUserRole(request));
    }
    
    @Operation(summary = "Get current user", description = "Returns the authenticated user's information")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User info retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserInfoRead.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<UserInfoRead> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication));
    }

}
