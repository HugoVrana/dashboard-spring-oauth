package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.auth.Grant;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface IGrantRepository extends MongoRepository<Grant, ObjectId> {
    Optional<Grant> findGrantBy_idAndAudit_DeletedAtIsNull(ObjectId id);
    Optional<Grant> getGrantByNameAndAudit_DeletedAtIsNull(String name);
}
