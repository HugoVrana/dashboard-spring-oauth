package com.dashboard.oauth.service;

import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.repository.IRoleRepository;
import com.dashboard.oauth.service.interfaces.IRoleService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService implements IRoleService {

    private final IRoleRepository roleRepository;

    @Override
    public Optional<Role> getRoleById(ObjectId id) {
        return roleRepository.findById(id);
    }

    @Override
    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByNameAndAudit_DeletedAtIsNull(name);
    }

    @Override
    public Role createRole(Role role) {
        return roleRepository.insert(role);
    }

    @Override
    public Role updateRole(Role role) {
        return roleRepository.save(role);
    }
}
