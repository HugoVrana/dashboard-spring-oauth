package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.role.RoleUpdate;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.repository.IGrantRepository;
import com.dashboard.oauth.repository.IRoleRepository;
import com.dashboard.oauth.service.interfaces.IRoleService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Scope("singleton")
@RequiredArgsConstructor
public class RoleService implements IRoleService {

    private final IRoleRepository roleRepository;
    private final IGrantRepository grantRepository;
    private final IRoleMapper roleMapper;

    @Override
    public List<RoleRead> getRoles() {
        return roleRepository
                .findByAudit_DeletedAtIsNull()
                .stream()
                .map(roleMapper::toRead)
                .toList();
    }

    @Override
    public Optional<Role> getRoleById(ObjectId id) {
        return roleRepository.findRoleBy_idAndAudit_DeletedAtIsNull(id);
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
    public RoleRead updateRole(ObjectId id, RoleUpdate update) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (!role.getName().equals(update.getName()) && getRoleByName(update.getName()).isPresent()) {
            throw new ConflictException("Role with this name already exists");
        }

        role.setName(update.getName());
        role.getAudit().setUpdatedAt(Instant.now());
        role = roleRepository.save(role);
        return roleMapper.toRead(role);
    }

    @Override
    public RoleRead addGrantToRole(ObjectId roleId, ObjectId grantId) {
        Role role = roleRepository.findRoleBy_idAndAudit_DeletedAtIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        Grant grant = grantRepository.findGrantBy_idAndAudit_DeletedAtIsNull(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Grant not found"));

        boolean alreadyAssigned = role.getGrants().stream()
                .anyMatch(g -> g.get_id().equals(grantId));
        if (alreadyAssigned) {
            throw new ConflictException("Grant is already assigned to this role");
        }

        role.getGrants().add(grant);
        role.getAudit().setUpdatedAt(Instant.now());
        role = roleRepository.save(role);
        return roleMapper.toRead(role);
    }

    @Override
    public RoleRead removeGrantFromRole(ObjectId roleId, ObjectId grantId) {
        Role role = roleRepository.findRoleBy_idAndAudit_DeletedAtIsNull(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        boolean removed = role.getGrants().removeIf(g -> g.get_id().equals(grantId));
        if (!removed) {
            throw new ResourceNotFoundException("Grant is not assigned to this role");
        }

        role.getAudit().setUpdatedAt(Instant.now());
        role = roleRepository.save(role);
        return roleMapper.toRead(role);
    }

    @Override
    public void deleteRole(ObjectId id) {
        Role role = roleRepository.findRoleBy_idAndAudit_DeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        roleRepository.delete(role);
    }
}
