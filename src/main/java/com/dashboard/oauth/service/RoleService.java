package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.repository.IRoleRepository;
import com.dashboard.oauth.service.interfaces.IRoleService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService implements IRoleService {

    private final IRoleRepository roleRepository;
    private final IRoleMapper roleMapper;

    @Override
    public List<RoleRead> getRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toRead)
                .toList();
    }

    @Override
    public Optional<Role> getRoleById(ObjectId id) {
        return roleRepository.findById(id);
    }

    @Override
    public RoleRead getRoleReadById(ObjectId id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        return roleMapper.toRead(role);
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
    public RoleRead createRole(CreateRole createRole) {
        if (getRoleByName(createRole.getName()).isPresent()) {
            throw new ConflictException("Role already exists");
        }
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        audit.setUpdatedAt(Instant.now());

        Role role = new Role();
        role.setName(createRole.getName());
        role.setAudit(audit);

        role = roleRepository.insert(role);
        return roleMapper.toRead(role);
    }

    @Override
    public Role updateRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public void deleteRole(ObjectId id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        roleRepository.delete(role);
    }
}
