package com.dashboard.oauth.datatransferobjects;

import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import org.bson.types.ObjectId;

import java.util.List;

public record UserInfo(
        ObjectId id,
        String email,
        List<Role> role
) {
    public static UserInfo fromUser(User user) {
        return new UserInfo(user.get_id(), user.getEmail(), user.getRoles());
    }
}

