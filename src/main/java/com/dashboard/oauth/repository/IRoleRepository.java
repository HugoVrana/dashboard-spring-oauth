package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.auth.Role;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface IRoleRepository extends MongoRepository<Role, ObjectId> {
    List<Role> findByAudit_DeletedAtIsNull();
    Optional<Role> findRoleBy_idAndAudit_DeletedAtIsNull(ObjectId id);
    Optional<Role> findByNameAndAudit_DeletedAtIsNull(String name);

}
