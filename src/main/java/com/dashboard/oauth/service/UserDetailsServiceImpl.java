package com.dashboard.oauth.service;

import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.repository.IGrantRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IDashboardUserDetailService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Scope("singleton")
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements IDashboardUserDetailService {

    private final IUserRepository userRepository;
    private final IGrantRepository grantRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndAudit_DeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        eagerLoadGrants(user);
        return new UserDetailsImpl(user);
    }

    private void eagerLoadGrants(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return;
        }

        List<ObjectId> allGrantIds = user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(Role::getGrants)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(Grant::get_id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (allGrantIds.isEmpty()) {
            return;
        }

        Map<ObjectId, Grant> grantsById = grantRepository.findAllById(allGrantIds).stream()
                .collect(Collectors.toMap(Grant::get_id, Function.identity()));

        user.getRoles().stream()
                .filter(Objects::nonNull)
                .forEach(role -> {
                    if (role.getGrants() != null) {
                        role.setGrants(role.getGrants().stream()
                                .filter(Objects::nonNull)
                                .map(g -> grantsById.getOrDefault(g.get_id(), g))
                                .toList());
                    }
                });
    }
}