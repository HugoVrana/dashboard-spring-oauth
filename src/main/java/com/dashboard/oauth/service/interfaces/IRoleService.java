package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.model.entities.Role;
import org.bson.types.ObjectId;
import java.util.Optional;

public interface IRoleService {
    Optional<Role> getRoleById(ObjectId id);
    Role createRole(Role role);
}
