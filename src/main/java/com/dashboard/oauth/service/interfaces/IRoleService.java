package com.dashboard.oauth.service.interfaces;

import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.role.RoleUpdate;
import com.dashboard.oauth.model.entities.auth.Role;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.Optional;

public interface IRoleService {
    List<RoleRead> getRoles();

    Optional<Role> getRoleById(ObjectId id);

    RoleRead getRoleReadById(ObjectId id);

    Optional<Role> getRoleByName(String name);

    Role createRole(Role role);

    RoleRead createRole(CreateRole createRole);

    RoleRead updateRole(ObjectId id, RoleUpdate update);

    RoleRead addGrantToRole(ObjectId roleId, ObjectId grantId);

    RoleRead removeGrantFromRole(ObjectId roleId, ObjectId grantId);

    void deleteRole(ObjectId id);
}
