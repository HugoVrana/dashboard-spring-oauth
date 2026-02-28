package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfUpdate;
import com.dashboard.oauth.model.entities.User;
import org.bson.types.ObjectId;

import java.util.Optional;

public interface IUserService {
    User saveUser(User user);

    Optional<User> getUserById(ObjectId id);

    Optional<User> getUserByEmail(String email);

    UserSelfRead getSelf(User user);

    UserSelfRead updateSelf(User user, UserSelfUpdate update);
}
