package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import org.bson.types.ObjectId;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.Optional;

public interface IDashboardUserDetailService extends UserDetailsService {
    Optional<User> getUserDetails(ObjectId userId);
    User addUserToRole(User user, Role role);
}
