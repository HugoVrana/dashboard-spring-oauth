package com.dashboard.oauth.controller;

import com.dashboard.oauth.datatransferobjects.*;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final IAuthenticationService authService;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<UserInfo> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            if (userDetails != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "User with this email already exists"
                );
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
