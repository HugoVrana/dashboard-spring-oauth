package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.model.entities.user.User;

public interface ILoginAttemptService {
    void checkLocked(User user);
    void recordFailedAttempt(User user);
    void recordSuccessfulLogin(User user);
}
