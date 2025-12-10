package com.dashboard.oauth.controller;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.*;
import com.dashboard.oauth.model.entities.ConflictException;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IDashboardUserDetailService;
import com.dashboard.oauth.service.interfaces.IRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final IAuthenticationService authService;
    private final IDashboardUserDetailService userDetailsService;
    private final IRoleService roleService;

    @PostMapping("/register")
    public ResponseEntity<UserInfo> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            if (userDetails != null) {
                throw new ConflictException("User with this email already exists");
            }
        }
        catch (UsernameNotFoundException notFoundException) {
            // User doesn't exist, we can proceed with registration
            UserInfo userInfo = authService.register(request);
            if (userInfo == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to register user"
                );
            }
            URI location = URI.create("/api/auth/register");
            return ResponseEntity.created(location).body(userInfo);
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        authService.logout(userDetails.getUser().get_id().toHexString());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/role")
    public ResponseEntity<Role> addRole(@Valid @RequestBody CreateRole createRole) {
        Role r = new Role();
        r.setName(createRole.getName());
        Audit a = new Audit();
        a.setCreatedAt(Instant.now());
        r.setAudit(a);
        r = roleService.createRole(r);
        return ResponseEntity.ok(r);
    }

    @PostMapping("/user/role")
    public ResponseEntity<AuthResponse> addUserRole(@Valid @RequestBody AddRoleRequest request) {
        if (!ObjectId.isValid(request.getUserId())) {
            throw new InvalidRequestException("User id is invalid.");
        }
        ObjectId userId = new ObjectId(request.getUserId());
        Optional<User> details = userDetailsService.getUserDetails(userId);
        if (details.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }

        User user = details.get();

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

        User updatedUser = userDetailsService.addUserToRole(user, roleToAdd);
        if (updatedUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to add role to user"
            );
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setId(updatedUser.get_id());
        userInfo.setEmail(updatedUser.getEmail());
        userInfo.setRole(updatedUser.getRoles());

        AuthResponse response = new AuthResponse();
        response.setUser(userInfo);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.get_id());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRoles());
        return ResponseEntity.ok(userInfo);
    }
}
