package com.dashboard.oauth.repository;

import com.dashboard.oauth.model.entities.oauth.AuthorizationRequest;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface IAuthorizationRequestRepository extends MongoRepository<AuthorizationRequest, ObjectId> {
    Optional<AuthorizationRequest> findBy_IdAndUsedTrueAndAudit_DeletedAtIsNull(ObjectId id);
}
