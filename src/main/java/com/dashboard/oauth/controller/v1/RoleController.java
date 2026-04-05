package com.dashboard.oauth.controller.v1;

import com.dashboard.common.model.ActivityEvent;
import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleGrantRequest;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.enums.ActivityEventType;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.dashboard.oauth.service.interfaces.IRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/role")
@Deprecated(since = "April 5th 2026" , forRemoval = true)
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService roleService;
    private final IGrantService grantService;
    private final IRoleMapper roleMapper;
    private final IGrantMapper grantMapper;
    private final IActivityFeedService activityFeedService;

    @PostMapping("/")
    public ResponseEntity<RoleRead> addRole(@Valid @RequestBody CreateRole createRole) {
        Optional<Role> role = roleService.getRoleByName(createRole.getName());
        if (role.isPresent()) {
            throw new ConflictException("Role already exists");
        }

        Role r = new Role();
        r.setName(createRole.getName());
        Audit a = new Audit();
        a.setCreatedAt(Instant.now());
        r.setAudit(a);
        r = roleService.createRole(r);

        ActivityEvent activityEvent = ActivityEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .type(ActivityEventType.ROLE_ADDED.name())
                .build();
        activityFeedService.publishEvent(activityEvent);

        RoleRead roleRead = roleMapper.toRead(r);
        return ResponseEntity.ok(roleRead);
    }

    @PostMapping("/grant")
    public ResponseEntity<RoleRead> addGrantToRole(@Valid @RequestBody RoleGrantRequest request) {
        if (!ObjectId.isValid(request.getRoleId())) {
            throw new InvalidRequestException("Role id is invalid.");
        }
        Optional<Role> role = roleService.getRoleById(new ObjectId(request.getRoleId()));
        if (role.isEmpty()) {
            throw new ResourceNotFoundException("Role not found");
        }
        Role roleToUpdate = role.get();

        if (!ObjectId.isValid(request.getGrantId())) {
            throw new InvalidRequestException("Grant id is invalid.");
        }
        Optional<Grant> grant = grantService.getGrantById(new ObjectId(request.getGrantId()));
        if (grant.isEmpty()) {
            throw new ResourceNotFoundException("Grant not found");
        }
        Grant grantToAdd = grant.get();

        if (roleToUpdate.getGrants() != null && roleToUpdate.getGrants().contains(grantToAdd)) {
            throw new ConflictException("Role already has this grant");
        }

        if (roleToUpdate.getGrants() == null) {
            roleToUpdate.setGrants(new ArrayList<>());
        }

        roleToUpdate.getGrants().add(grantToAdd);
        roleToUpdate.getAudit().setUpdatedAt(Instant.now());
        Role updatedRole = roleService.updateRole(roleToUpdate);

        ActivityEvent activityEvent = ActivityEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .type(ActivityEventType.GRANT_ADDED_TO_ROLE.name())
                .build();
        activityFeedService.publishEvent(activityEvent);

        RoleRead roleRead = roleMapper.toRead(updatedRole);
        List<GrantRead> grants = new ArrayList<>();
        for (Grant g : updatedRole.getGrants()) {
            GrantRead grantRead = grantMapper.toRead(g);
            grants.add(grantRead);
        }
        roleRead.setGrants(grants);
        return ResponseEntity.ok(roleRead);
    }

    @DeleteMapping("/grant")
    public ResponseEntity<Integer> removeGrantFromRole(@Valid @RequestBody RoleGrantRequest request) {
        if (!ObjectId.isValid(request.getRoleId())) {
            throw new InvalidRequestException("Role id is invalid.");
        }
        Optional<Role> role = roleService.getRoleById(new ObjectId(request.getRoleId()));
        if (role.isEmpty()) {
            throw new ResourceNotFoundException("Role not found");
        }
        Role roleToUpdate = role.get();
        if (roleToUpdate.getGrants() == null) {
            return ResponseEntity.ok(0);
        }
        if (!ObjectId.isValid(request.getGrantId())) {
            throw new InvalidRequestException("Grant id is invalid.");
        }

        Integer before = roleToUpdate.getGrants().size();

        Optional<Grant> grant = grantService.getGrantById(new ObjectId(request.getGrantId()));
        if (grant.isEmpty()) {
            throw new ResourceNotFoundException("Grant not found");
        }
        Grant grantToRemove = grant.get();
        roleToUpdate.getGrants().remove(grantToRemove);
        roleToUpdate.getAudit().setUpdatedAt(Instant.now());
        Role updatedRole = roleService.updateRole(roleToUpdate);
        Integer after = updatedRole.getGrants().size();
        Integer countAffected = before - after;

        if (countAffected != 0) {
            ActivityEvent activityEvent = ActivityEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .type(ActivityEventType.GRANT_REMOVED_FROM_ROLE.name())
                    .build();
            activityFeedService.publishEvent(activityEvent);
        }

        return ResponseEntity.ok(countAffected);
    }
}