package com.dashboard.oauth.controller;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RefreshTokenRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.dataTransferObject.role.AddRoleRequest;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.Grant;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final IAuthenticationService authService;
    private final IDashboardUserDetailService userDetailsService;
    private final IUserService userService;
    private final IRoleService roleService;
    private final IJwtService jwtService;
    private final IUserInfoMapper userInfoMapper;
    private final IGrantMapper grantMapper;
    private final IRoleMapper roleMapper;

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

            // check if the role id is valid
            if (!ObjectId.isValid(request.getRoleId())) {
                throw new InvalidRequestException("Role id is invalid.");
            }

            ObjectId roleId = new ObjectId(request.getRoleId());
            Optional<Role> role = roleService.getRoleById(roleId);
            if  (role.isEmpty()) {
                throw new ResourceNotFoundException("Role with id " + roleId + " not found");
            }

            User user = authService.register(request);
            if (user == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to register user"
                );
            }

            List<Role> roles = user.getRoles();
            roles.add(role.get());
            user.setRoles(roles);
            user = userService.saveUser(user);

            if (user == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to add role to user"
                );
            }

            UserInfo userInfo = userInfoMapper.toUserInfo(user);

            URI location = URI.create("/api/auth/register");

            UserInfoRead infoRead = userInfoMapper.toRead(userInfo);
            List<RoleRead> roleReadList = new ArrayList<>();
            for (Role r :  user.getRoles()) {
                RoleRead rr = roleMapper.toRead(r);

                List<GrantRead> grants = new ArrayList<>();
                for (Grant g : r.getGrants()) {
                    GrantRead gr = grantMapper.toRead(g);
                    grants.add(gr);
                }
                rr.setGrants(grants);

                roleReadList.add(rr);
            }
            infoRead.setRoleReads(roleReadList.toArray(new RoleRead[0]));

            return ResponseEntity.created(location).body(infoRead);
        }
        return ResponseEntity.badRequest().build();
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