package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.user.UserAdminRead;
import com.dashboard.oauth.dataTransferObject.user.UserAdminUpdate;
import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfUpdate;
import com.dashboard.oauth.model.entities.user.User;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    User saveUser(User user);

    Optional<User> getUserById(ObjectId id);

    Optional<User> getUserByEmail(String email);

    UserSelfRead getSelf(User user);

    UserSelfRead updateSelf(User user, UserSelfUpdate update);

    List<UserAdminRead> getUsers();

    List<UserAdminRead> searchUsers(String query);

    UserAdminRead getUserAdminReadById(ObjectId id);

    UserAdminRead updateUser(ObjectId id, UserAdminUpdate update);

    void deleteUser(ObjectId id);

    void blockUser(ObjectId id);

    void unblockUser(ObjectId id);

    void resendVerificationEmail(ObjectId id);

    void triggerPasswordReset(ObjectId id);
}
