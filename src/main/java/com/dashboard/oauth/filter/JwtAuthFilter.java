package com.dashboard.oauth.filter;

import com.dashboard.oauth.authentication.GrantsAuthentication;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.service.JwtService;
import com.dashboard.oauth.service.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.validateToken(jwt, userDetails)) {
                    User user = ((UserDetailsImpl) userDetails).getUser();
                    List<String> grants = extractGrants(user);
                    String profileImageId = user.getProfileImageId() != null
                            ? user.getProfileImageId().toHexString() : "";

                    GrantsAuthentication authToken = new GrantsAuthentication(
                            user.getEmail(),
                            user.get_id().toHexString(),
                            profileImageId,
                            grants
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log the error but don't block the filter chain
            logger.error("Cannot set user authentication: { " + e.getMessage() + "}");
        }

        filterChain.doFilter(request, response);
    }

    private List<String> extractGrants(User user) {
        if (user.getRoles() == null) {
            return List.of();
        }

        return user.getRoles().stream()
                .filter(role -> role != null && role.getName() != null)
                .flatMap(role -> Stream.concat(
                        Stream.of("ROLE_" + role.getName()),
                        grantsFromRole(role)
                ))
                .toList();
    }

    private Stream<String> grantsFromRole(Role role) {
        if (role.getGrants() == null) {
            return Stream.empty();
        }
        return role.getGrants().stream()
                .filter(Objects::nonNull)
                .map(Grant::getName)
                .filter(Objects::nonNull);
    }
}