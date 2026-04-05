package com.dashboard.oauth.service;

import com.dashboard.oauth.environment.LoginProperties;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.ILoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@Scope("singleton")
@RequiredArgsConstructor
public class LoginAttemptService implements ILoginAttemptService {

    private final IUserRepository userRepository;
    private final LoginProperties loginProperties;

    @Override
    public void checkLocked(User user) {
        if (user.isLocked()) {
            throw new LockedException("User account is locked");
        }
    }

    @Override
    public void recordFailedAttempt(User user) {
        Instant now = Instant.now();
        userRepository.incrementFailedLoginAttempts(user.get_id(), now);

        int currentAttempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
        if (currentAttempts + 1 >= loginProperties.getMaxFailedAttempts()) {
            userRepository.lockUser(user.get_id(), now);
        }
    }

    @Override
    public void recordSuccessfulLogin(User user) {
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            userRepository.resetFailedLoginAttempts(user.get_id());
        }
    }
}
