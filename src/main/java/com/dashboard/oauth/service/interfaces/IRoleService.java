package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.model.entities.Role;
import org.bson.types.ObjectId;
import java.util.Optional;

public interface IRoleService {
    Optional<Role> getRoleById(ObjectId id);
    Optional<Role> getRoleByName(String name);
    Role createRole(Role role);
    Role updateRole(Role role);
}
