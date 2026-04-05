package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.auth.Role;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface IRoleRepository extends MongoRepository<Role, ObjectId> {
    Optional<Role> findByNameAndAudit_DeletedAtIsNull(String name);
}
