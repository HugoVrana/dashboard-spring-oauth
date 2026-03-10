package com.dashboard.oauth.authentication;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GrantsAuthentication implements Authentication {
    private final String username;
    @Getter
    private final String userId;
    @Getter
    private final String profileImageUrl;
    private final List<String> grants;
    private boolean authenticated = true;

    /**
     * Retrieves the current GrantsAuthentication from the SecurityContext.
     * Should only be called from endpoints protected by @PreAuthorize.
     *
     * @return the current GrantsAuthentication
     * @throws IllegalStateException if no authentication is present (indicates a programming error)
     */
    public static GrantsAuthentication current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof GrantsAuthentication grantsAuth) {
            return grantsAuth;
        }
        throw new IllegalStateException("No GrantsAuthentication in SecurityContext - this method should only be called from authenticated endpoints");
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return grants.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return username;
    }

    public String getProfileImageUrlOrEmpty() {
        return profileImageUrl != null && !profileImageUrl.isEmpty() ? profileImageUrl : "";
    }
}
