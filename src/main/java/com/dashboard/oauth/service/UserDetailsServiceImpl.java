package com.dashboard.oauth.service;

import com.dashboard.common.model.exception.NotFoundException;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IDashboardUserDetailService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements IDashboardUserDetailService {

    private final IUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new UserDetailsImpl(user);
    }

    @Override
    public Optional<User> getUserDetails(ObjectId userId) throws NotFoundException {
        return userRepository.findById(userId);
    }

    @Override
    public User addUserToRole(User user, Role role) {
        user.getRoles().add(role);
        user.getAudit().setUpdatedAt(Instant.now());
        user = userRepository.save(user);
        return user;
    }
}

