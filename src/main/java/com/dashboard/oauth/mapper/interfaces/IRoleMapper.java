package com.dashboard.oauth.mapper.interfaces;

import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.model.entities.auth.Role;

public interface IRoleMapper {
    RoleRead toRead(Role role);
}

