package com.dashboard.oauth.mapper;

import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.model.entities.auth.Role;
import org.springframework.stereotype.Service;

@Service
public final class RoleMapper implements IRoleMapper {
    @Override
    public RoleRead toRead(final Role role) {
        RoleRead roleRead = new RoleRead();
        roleRead.setId(role.get_id().toHexString());
        roleRead.setName(role.getName());
        return roleRead;
    }
}