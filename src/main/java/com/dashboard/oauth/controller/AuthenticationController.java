package com.dashboard.oauth.controller;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RefreshTokenRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.dataTransferObject.role.AddRoleRequest;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.entities.ConflictException;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.service.UserDetailsImpl;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IDashboardUserDetailService;
import com.dashboard.oauth.service.interfaces.IGrantService;
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
    private final IGrantService grantService;
    private final IUserInfoMapper userInfoMapper;
    private final IRoleMapper roleMapper;
    private final IGrantMapper grantMapper;

    @PostMapping("/register")
    public ResponseEntity<UserInfoRead> register(@Valid @RequestBody RegisterRequest request) {
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

            UserInfoRead infoRead = userInfoMapper.toRead(userInfo);
            return ResponseEntity.created(location).body(infoRead);
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
    public ResponseEntity<RoleRead> addRole(@Valid @RequestBody CreateRole createRole) {
        Optional<Role> role = roleService.getRoleByName(createRole.getName());
        if (role.isPresent()) {
            throw new ConflictException("Role already exists");
        }

        Role r = new Role();
        r.setName(createRole.getName());
        Audit a = new Audit();
        a.setCreatedAt(Instant.now());
        r.setAudit(a);
        r = roleService.createRole(r);

        RoleRead roleRead = roleMapper.toRead(r);
        return ResponseEntity.ok(roleRead);
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

    @PostMapping("/grand")
    public ResponseEntity<GrantRead> addGrant(@Valid @RequestBody GrantCreate grantCreate) {
        Optional<Grant> grant = grantService.getGrantByName(grantCreate.getName());
        if (grant.isPresent()) {
            throw new ConflictException("Grant already exists");
        }

        Grant g = new Grant();
        g.setName(grantCreate.getName());
        g.setDescription(grantCreate.getDescription());
        Audit a = new Audit();
        a.setCreatedAt(Instant.now());
        g.setAudit(a);
        g = grantService.createGrant(g);
        GrantRead grantRead = grantMapper.toRead(g);
        return ResponseEntity.ok(grantRead);
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
