package com.dashboard.oauth.controller;

import com.dashboard.oauth.dataTransferObject.auth.TokenIntrospectionRequest;
import com.dashboard.oauth.dataTransferObject.auth.TokenIntrospectionResponse;
import com.dashboard.oauth.environment.Oauth2Properties;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.dashboard.oauth.service.interfaces.IJwtService;
import com.dashboard.oauth.service.interfaces.IUserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final Oauth2Properties properties;
    private final IUserService userService;
    private final IJwtService jwtService;
    private final IGrantService grantService;

    @PostMapping("/introspect")
    public TokenIntrospectionResponse introspect(
            @RequestBody TokenIntrospectionRequest request,
            @RequestHeader("X-Service-Secret") String secret) {

        if (!MessageDigest.isEqual(
                properties.getSecret().getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        try {
            List<String> grantNames = jwtService.extractClaim(request.getToken(), c ->
                    ((List<?>) c.get("grants")).stream()
                            .map(String::valueOf)
                            .toList()
            );
            String email = jwtService.extractUsername(request.getToken());
            Date expiration = jwtService.extractClaim(request.getToken(), Claims::getExpiration);
            boolean isActive = expiration.after(new Date());

            Optional<User> optionalUser = userService.getUserByEmail(email);
            if (optionalUser.isEmpty()) {
                TokenIntrospectionResponse response = new TokenIntrospectionResponse();
                response.setSubject(email);
                response.setIsActive(false);
                return response;
            }

            // invalidate if the user got deleted
            User user = optionalUser.get();
            if (user.getAudit().getDeletedAt() != null) {
                TokenIntrospectionResponse response = new TokenIntrospectionResponse();
                response.setSubject(email);
                response.setIsActive(false);
                return response;
            }

            // only return the valid grants
            List<String> activeGrants = new ArrayList<>();
            for (String grantName : grantNames) {
                Optional<Grant> optional = grantService.getGrantByName(grantName);
                if (optional.isEmpty()) {
                    isActive = false;
                    break;
                }

                if (optional.get().getAudit().getDeletedAt() != null) {
                    isActive = false;
                    break;
                }

                activeGrants.add(grantName);
            }

            TokenIntrospectionResponse response = new TokenIntrospectionResponse();
            response.setGrants(activeGrants);
            response.setSubject(email);
            response.setExpiration(expiration.toInstant().toEpochMilli());
            response.setIsActive(isActive);
            return response;
        } catch (JwtException e) {
            return new TokenIntrospectionResponse();
        }
    }
}