package com.dashboard.oauth.controller;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.ForgotPasswordRequest;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RefreshTokenRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.auth.ResetPasswordRequest;
import com.dashboard.oauth.dataTransferObject.role.AddRoleRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
import com.dashboard.oauth.service.interfaces.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final IAuthenticationService authService;
    private final IUserService userService;
    private final IRoleService roleService;
    private final IJwtService jwtService;

    private final IUserInfoMapper userInfoMapper;

    @PostMapping("/register")
    public ResponseEntity<UserInfoRead> register(@Valid @RequestBody RegisterRequest request) {
        UserInfoRead response = authService.register(request);
        URI location = URI.create("/api/auth/register");
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        // Extract token from "Bearer <token>"
        String token = authHeader.substring(7);
        // Decode the JWT to get the user ID, or use a service to invalidate the token
        String email = jwtService.extractUsername(token);
        Optional<User> optionalUser = userService.getUserByEmail(email);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        User user = optionalUser.get();
        authService.logout(user.get_id().toHexString());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/role")
    public ResponseEntity<AuthResponse> addUserRole(@Valid @RequestBody AddRoleRequest request) {
        if (!ObjectId.isValid(request.getUserId())) {
            throw new InvalidRequestException("User id is invalid.");
        }
        ObjectId userId = new ObjectId(request.getUserId());

        Optional<User> optionalUser = userService.getUserById(userId);
        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }

        User user = optionalUser.get();

        if (!ObjectId.isValid(request.getRoleId())) {
            throw new InvalidRequestException("Role id is invalid.");
        }
        ObjectId roleId = new ObjectId(request.getRoleId());
        Optional<Role> role = roleService.getRoleById(roleId);
        if (role.isEmpty()) {
            throw new ResourceNotFoundException("Role not found");
        }

        Role roleToAdd = role.get();

        if (user.getRoles().contains(roleToAdd)){
            throw new ConflictException("User already has this role");
        }

        user.getRoles().add(roleToAdd);
        user = userService.saveUser(user);
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to add role to user"
            );
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.get_id());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRoles());

        UserInfoRead userInfoRead = userInfoMapper.toRead(userInfo);

        AuthResponse response = new AuthResponse();
        response.setUser(userInfoRead);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoRead> getCurrentUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.get_id());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRoles());

        UserInfoRead userInfoRead = userInfoMapper.toRead(userInfo);

        return ResponseEntity.ok(userInfoRead);
    }
}