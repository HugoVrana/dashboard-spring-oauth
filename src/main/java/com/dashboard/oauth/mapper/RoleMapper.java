package com.dashboard.oauth.mapper;

import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.model.entities.auth.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public final class RoleMapper implements IRoleMapper {

    private final IGrantMapper grantMapper;

    @Override
    public RoleRead toRead(final Role role) {
        RoleRead roleRead = new RoleRead();
        roleRead.setId(role.get_id().toHexString());
        roleRead.setName(role.getName());
        if (role.getGrants() != null) {
            List<GrantRead> grants = role.getGrants().stream()
                    .map(grantMapper::toRead)
                    .toList();
            roleRead.setGrants(grants);
        }
        return roleRead;
    }
}