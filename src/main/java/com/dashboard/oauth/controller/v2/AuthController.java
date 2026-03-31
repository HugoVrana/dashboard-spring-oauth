package com.dashboard.oauth.controller.v2;

import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@CrossOrigin
@RestController("v2AuthController")
@RequiredArgsConstructor
@RequestMapping("api/v2/auth")
@Tag(name = "Authentication (v2)", description = "Authentication operations")
public class AuthController {

    private final IAuthenticationService authService;
    private final IOAuthClientService oAuthClientService;

    @Operation(summary = "Register a new user", description = "Creates a new user account with email, password, and role")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = UserInfoRead.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid X-Client-Id header"),
            @ApiResponse(responseCode = "403", description = "Origin not allowed for this client"),
            @ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<UserInfoRead> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        validateClientAccess(httpRequest);
        UserInfoRead response = authService.register(request);
        return ResponseEntity.created(URI.create("/api/v2/auth/register")).body(response);
    }

    private void validateClientAccess(HttpServletRequest request) {
        String clientId = request.getHeader("X-Client-Id");
        if (!oAuthClientService.isRegisteredClient(clientId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A registered X-Client-Id header is required");
        }
        if (!oAuthClientService.isAllowedHost(clientId, request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The request origin is not allowed for this client");
        }
    }
}
